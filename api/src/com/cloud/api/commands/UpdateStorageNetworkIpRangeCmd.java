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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.StorageNetworkIpRangeResponse;
import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@Implementation(description="Update a Storage network IP range, only allowed when no IPs in this range have been allocated.", responseObject=StorageNetworkIpRangeResponse.class, since="3.0.0")
public class UpdateStorageNetworkIpRangeCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(UpdateStorageNetworkIpRangeCmd.class);
	private static final String s_name = "updatestoragenetworkiprangeresponse";
	
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	@IdentityMapper(entityTableName="dc_storage_network_ip_range")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="UUID of storage network ip range")
    private Long id;
	
    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, description="the beginning IP address")
    private String startIp;

    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address")
    private String endIp;
    
    @Parameter(name=ApiConstants.VLAN, type=CommandType.INTEGER, description="Optional. the vlan the ip range sits on")
    private Integer vlan;
    
    @Parameter(name=ApiConstants.NETMASK, type=CommandType.STRING, description="the netmask for storage network")
    private String netmask;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public String getEndIp() {
        return endIp;
    }

    public String getStartIp() {
        return startIp;
    }

    public Integer getVlan() {
        return vlan;
    }
    
    public String getNetmask() {
    	return netmask;
    }

    public Long getId() {
    	return id;
    }
    
	@Override
	public String getEventType() {
		return EventTypes.EVENT_STORAGE_IP_RANGE_UPDATE;
	}

	@Override
	public String getEventDescription() {
		return "Update storage ip range " + getId() + " [StartIp=" + getStartIp() + ", EndIp=" + getEndIp() + ", vlan=" + getVlan() + ", netmask=" + getNetmask() + ']';
	}

	@Override
	public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
	        ResourceAllocationException {
		try {
			StorageNetworkIpRange result = _storageNetworkService.updateIpRange(this);
			StorageNetworkIpRangeResponse response = _responseGenerator.createStorageNetworkIpRangeResponse(result);
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (Exception e) {
			s_logger.warn("Update storage network IP range failed", e);
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
		}

	}

	@Override
	public String getCommandName() {
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		return Account.ACCOUNT_ID_SYSTEM;
	}

}

