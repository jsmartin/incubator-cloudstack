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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListDomainResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@Implementation(description="Lists accounts and provides detailed account information for listed accounts", responseObject=AccountResponse.class)
public class ListAccountsCmd extends BaseListDomainResourcesCmd {
	public static final Logger s_logger = Logger.getLogger(ListAccountsCmd.class.getName());
    private static final String s_name = "listaccountsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT_TYPE, type=CommandType.LONG, description="list accounts by account type. Valid account types are 1 (admin), 2 (domain-admin), and 0 (user).")
    private Long accountType;

    @IdentityMapper(entityTableName="account")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list account by account ID")
    private Long id;

    @Parameter(name=ApiConstants.IS_CLEANUP_REQUIRED, type=CommandType.BOOLEAN, description="list accounts by cleanuprequred attribute (values are true or false)")
    private Boolean cleanupRequired;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list account by account name")
    private String searchName;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="list accounts by state. Valid states are enabled, disabled, and locked.")
    private String state;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getAccountType() {
        return accountType;
    }

    public Long getId() {
        return id;
    }

    public Boolean isCleanupRequired() {
        return cleanupRequired;
    }

    public String getSearchName() {
        return searchName;
    }

    public String getState() {
        return state;
    }
    

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        Pair<List<? extends Account>, Integer> accounts = _accountService.searchForAccounts(this);
        ListResponse<AccountResponse> response = new ListResponse<AccountResponse>();
        List<AccountResponse> accountResponses = new ArrayList<AccountResponse>();
        for (Account account : accounts.first()) {
            AccountResponse acctResponse = _responseGenerator.createAccountResponse(account);
            acctResponse.setObjectName("account");
            accountResponses.add(acctResponse);
        }
        response.setResponses(accountResponses, accounts.second());
        response.setResponseName(getCommandName());
        
        this.setResponseObject(response);
    }
}
