#!/usr/bin/env python



from cloudstackTestCase import *
from remoteSSHClient import remoteSSHClient 

import unittest
import hashlib
import random
import string

class TestDeployVm(cloudstackTestCase):
    """
    This test deploys a virtual machine into a user account 
    using the small service offering and builtin template
    """
    @classmethod
    def setUpClass(cls):
        """
        CloudStack internally saves its passwords in md5 form and that is how we
        specify it in the API. Python's hashlib library helps us to quickly hash
        strings as follows
        """
        mdf = hashlib.md5()
        mdf.update('password')
        mdf_pass = mdf.hexdigest()
        acctName = 'bugs-'+''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(6)) #randomly generated account

        cls.apiClient = super(TestDeployVm, cls).getClsTestClient().getApiClient() 
        cls.acct = createAccount.createAccountCmd() #The createAccount command
        cls.acct.accounttype = 0                    #We need a regular user. admins have accounttype=1
        cls.acct.firstname = 'bugs'                 
        cls.acct.lastname = 'bunny'                 #What's up doc?
        cls.acct.password = mdf_pass                #The md5 hashed password string
        cls.acct.username = acctName
        cls.acct.email = 'bugs@rabbithole.com'
        cls.acct.account = acctName
        cls.acct.domainid = 1                       #The default ROOT domain
        cls.acctResponse = cls.apiClient.createAccount(cls.acct)
        
    def setUpNAT(self, virtualmachineid):
        listSourceNat = listPublicIpAddresses.listPublicIpAddressesCmd()
        listSourceNat.account = self.acct.account
        listSourceNat.domainid = self.acct.domainid
        listSourceNat.issourcenat = True
        
        listsnatresponse = self.apiClient.listPublicIpAddresses(listSourceNat)
        self.assertNotEqual(len(listsnatresponse), 0, "Found a source NAT for the acct %s"%self.acct.account)
        
        snatid = listsnatresponse[0].id
        snatip = listsnatresponse[0].ipaddress
        
        try:
            createFwRule = createFirewallRule.createFirewallRuleCmd()
            createFwRule.cidrlist = "0.0.0.0/0"
            createFwRule.startport = 22
            createFwRule.endport = 22
            createFwRule.ipaddressid = snatid
            createFwRule.protocol = "tcp"
            createfwresponse = self.apiClient.createFirewallRule(createFwRule)
            
            createPfRule = createPortForwardingRule.createPortForwardingRuleCmd()
            createPfRule.privateport = 22
            createPfRule.publicport = 22
            createPfRule.virtualmachineid = virtualmachineid
            createPfRule.ipaddressid = snatid
            createPfRule.protocol = "tcp"
            
            createpfresponse = self.apiClient.createPortForwardingRule(createPfRule)
        except e:
            self.debug("Failed to create PF rule in account %s due to %s"%(self.acct.account, e))
            raise e
        finally:
            return snatip        

    def test_DeployVm(self):
        """
        Let's start by defining the attributes of our VM that we will be
        deploying on CloudStack. We will be assuming a single zone is available
        and is configured and all templates are Ready

        The hardcoded values are used only for brevity. 
        """
        deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVmCmd.zoneid = 1
        deployVmCmd.account = self.acct.account
        deployVmCmd.domainid = self.acct.domainid
        deployVmCmd.templateid = 2
        deployVmCmd.serviceofferingid = 1

        deployVmResponse = self.apiClient.deployVirtualMachine(deployVmCmd)
        self.debug("VM %s was deployed in the job %s"%(deployVmResponse.id, deployVmResponse.jobid))

        # At this point our VM is expected to be Running. Let's find out what
        # listVirtualMachines tells us about VMs in this account

        listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
        listVmCmd.id = deployVmResponse.id
        listVmResponse = self.apiClient.listVirtualMachines(listVmCmd)

        self.assertNotEqual(len(listVmResponse), 0, "Check if the list API \
                            returns a non-empty response")

        vm = listVmResponse[0]
        hostname = vm.name
        nattedip = self.setUpNAT(vm.id)

        self.assertEqual(vm.id, deployVmResponse.id, "Check if the VM returned \
                         is the same as the one we deployed")


        self.assertEqual(vm.state, "Running", "Check if VM has reached \
                         a state of running")

        # SSH login and compare hostname        
        ssh_client = remoteSSHClient(nattedip, 22, "root", "password")
        stdout = ssh_client.execute("hostname")
        
        self.assertEqual(hostname, stdout[0], "cloudstack VM name and hostname match")


    @classmethod
    def tearDownClass(cls):
        """
        And finally let us cleanup the resources we created by deleting the
        account. All good unittests are atomic and rerunnable this way
        """
        deleteAcct = deleteAccount.deleteAccountCmd()
        deleteAcct.id = cls.acctResponse.account.id
        cls.apiClient.deleteAccount(deleteAcct)
