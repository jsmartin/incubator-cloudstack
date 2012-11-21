package org.apache.cloudstack.storage.strategy;

import org.apache.cloudstack.engine.subsystem.api.storage.BackupStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;

public class XenBackupStrategy implements BackupStrategy {
    protected DataStore _ds;

    public XenBackupStrategy(DataStore ds) {
        _ds = ds;
    }
}