// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.api.response.SecurityGroupRuleResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.security.SecurityRule;
import com.cloud.user.UserContext;
import com.cloud.utils.StringUtils;
import com.cloud.utils.net.NetUtils;

@Implementation(responseObject = SecurityGroupRuleResponse.class, description = "Authorizes a particular ingress rule for this security group")
@SuppressWarnings("rawtypes")
public class AuthorizeSecurityGroupIngressCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AuthorizeSecurityGroupIngressCmd.class.getName());

    private static final String s_name = "authorizesecuritygroupingressresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "TCP is default. UDP is the other supported protocol")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "start port for this ingress rule")
    private Integer startPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "end port for this ingress rule")
    private Integer endPort;

    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the icmp message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this icmp message")
    private Integer icmpCode;

    @Parameter(name=ApiConstants.CIDR_LIST, type=CommandType.LIST, collectionType=CommandType.STRING, description="the cidr list associated")
    private List<String> cidrList;

    @Parameter(name = ApiConstants.USER_SECURITY_GROUP_LIST, type = CommandType.MAP, description = "user to security group mapping")
    private Map userSecurityGroupList;
    
    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the security group. If the account parameter is used, domainId must also be used.")
    private Long domainId;
    
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the security group. Must be used with domainId.")
    private String accountName;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="an optional project of the security group")
    private Long projectId;
    
    @IdentityMapper(entityTableName="security_group")
    @Parameter(name=ApiConstants.SECURITY_GROUP_ID, type=CommandType.LONG, description="The ID of the security group. Mutually exclusive with securityGroupName parameter")
    private Long securityGroupId;
    
    @Parameter(name=ApiConstants.SECURITY_GROUP_NAME, type=CommandType.STRING, description="The name of the security group. Mutually exclusive with securityGroupName parameter")
    private String securityGroupName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public List<String> getCidrList() {
        return cidrList;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public Long getSecurityGroupId() {
        if (securityGroupId != null && securityGroupName != null) {
            throw new InvalidParameterValueException("securityGroupId and securityGroupName parameters are mutually exclusive");
        }
        
        if (securityGroupName != null) {
            securityGroupId = _responseGenerator.getSecurityGroupId(securityGroupName, getEntityOwnerId());
            if (securityGroupId == null) {
                throw new InvalidParameterValueException("Unable to find security group " + securityGroupName + " for account id=" + getEntityOwnerId());
            }
            securityGroupName = null;
        }
        
        if (securityGroupId == null) {
            throw new InvalidParameterValueException("Either securityGroupId or securityGroupName is required by authorizeSecurityGroupIngress command");
        }
        
        return securityGroupId;
    }

    public String getProtocol() {
        if (protocol == null) {
            return "all";
        }
        return protocol;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public Map getUserSecurityGroupList() {
        return userSecurityGroupList;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "securitygroup";
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        
        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SECURITY_GROUP_AUTHORIZE_INGRESS;
    }

    @Override
    public String getEventDescription() {
        StringBuilder sb = new StringBuilder();
        if (getUserSecurityGroupList() != null) {
            sb.append("group list(group/account): ");
            Collection userGroupCollection = getUserSecurityGroupList().values();
            Iterator iter = userGroupCollection.iterator();

            HashMap userGroup = (HashMap) iter.next();
            String group = (String) userGroup.get("group");
            String authorizedAccountName = (String) userGroup.get("account");
            sb.append(group + "/" + authorizedAccountName);

            while (iter.hasNext()) {
                userGroup = (HashMap) iter.next();
                group = (String) userGroup.get("group");
                authorizedAccountName = (String) userGroup.get("account");
                sb.append(", " + group + "/" + authorizedAccountName);
            }
        } else if (getCidrList() != null) {
            sb.append("cidr list: ");
            sb.append(StringUtils.join(getCidrList(), ", "));
        } else {
            sb.append("<error:  no ingress parameters>");
        }

        return "authorizing ingress to group: " + getSecurityGroupId() + " to " + sb.toString();
    }

    @Override
    public void execute() {
        if(cidrList != null){
            for(String cidr : cidrList ){	
                if (!NetUtils.isValidCIDR(cidr)){
                    throw new ServerApiException(BaseCmd.PARAM_ERROR,  cidr + " is an Invalid CIDR ");
                }
            }
        }
        List<? extends SecurityRule> ingressRules = _securityGroupService.authorizeSecurityGroupIngress(this);
        if (ingressRules != null && !ingressRules.isEmpty()) {
            SecurityGroupResponse response = _responseGenerator.createSecurityGroupResponseFromSecurityGroupRule(ingressRules);
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to authorize security group ingress rule(s)");
        }
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.SecurityGroup;
    }

    @Override
    public Long getInstanceId() {
        return getSecurityGroupId();
    }
}
