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
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.LBStickinessResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description = "Lists LBStickiness policies.", responseObject = LBStickinessResponse.class, since="3.0.0")
public class ListLBStickinessPoliciesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger
            .getLogger(ListLBStickinessPoliciesCmd.class.getName());

    private static final String s_name = "listlbstickinesspoliciesresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @IdentityMapper(entityTableName="firewall_rules")
    @Parameter(name = ApiConstants.LBID, type = CommandType.LONG, required = true, description = "the ID of the load balancer rule")
    private Long lbRuleId;
    


    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    public Long getLbRuleId() {
        return lbRuleId;
    }
    


    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        List<LBStickinessResponse> spResponses = new ArrayList<LBStickinessResponse>();
        LoadBalancer lb = _lbService.findById(getLbRuleId());
        ListResponse<LBStickinessResponse> response = new ListResponse<LBStickinessResponse>();
        
        if (lb != null) {
        	//check permissions
        	Account caller = UserContext.current().getCaller();
        	_accountService.checkAccess(caller, null, true, lb);
            List<? extends StickinessPolicy> stickinessPolicies = _lbService.searchForLBStickinessPolicies(this);
            LBStickinessResponse spResponse = _responseGenerator.createLBStickinessPolicyResponse(stickinessPolicies, lb);
            spResponses.add(spResponse);
            response.setResponses(spResponses);
        }
        
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
