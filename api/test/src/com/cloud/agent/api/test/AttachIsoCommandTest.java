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
package src.com.cloud.agent.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.cloud.agent.api.AttachIsoCommand;

public class AttachIsoCommandTest {
    AttachIsoCommand aic = new AttachIsoCommand("vmname", "isopath", false);

    @Test
    public void testGetVmName() {
        String vmName = aic.getVmName();
        assertTrue(vmName.equals("vmname"));
    }

    @Test
    public void testGetIsoPath() {
        String isoPath = aic.getIsoPath();
        assertTrue(isoPath.equals("isopath"));
    }

    @Test
    public void testIsAttach() {
        boolean b = aic.isAttach();
        assertFalse(b);
    }

    @Test
    public void testGetStoreUrl() {
        aic.setStoreUrl("http://incubator.apache.org/cloudstack/");
        String url = aic.getStoreUrl();
        assertTrue(url.equals("http://incubator.apache.org/cloudstack/"));
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = aic.executeInSequence();
        assertTrue(b);
    }

    @Test
    public void testAllowCaching() {
        boolean b = aic.allowCaching();
        assertTrue(b);
    }

    @Test
    public void testGetWait() {
        int b;
        aic.setWait(5);
        b = aic.getWait();
        assertEquals(b, 5);
        aic.setWait(-3);
        b = aic.getWait();
        assertEquals(b, -3);
        aic.setWait(0);
        b = aic.getWait();
        assertEquals(b, 0);
    }
}
