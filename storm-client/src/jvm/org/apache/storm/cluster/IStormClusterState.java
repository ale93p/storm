/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.storm.cluster;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.storm.generated.Assignment;
import org.apache.storm.generated.ClusterWorkerHeartbeat;
import org.apache.storm.generated.Credentials;
import org.apache.storm.generated.ErrorInfo;
import org.apache.storm.generated.ExecutorInfo;
import org.apache.storm.generated.LogConfig;
import org.apache.storm.generated.NimbusSummary;
import org.apache.storm.generated.NodeInfo;
import org.apache.storm.generated.PrivateWorkerKey;
import org.apache.storm.generated.ProfileRequest;
import org.apache.storm.generated.StormBase;
import org.apache.storm.generated.SupervisorInfo;
import org.apache.storm.generated.WorkerTokenServiceType;
import org.apache.storm.nimbus.NimbusInfo;

public interface IStormClusterState {
    public List<String> assignments(Runnable callback);

    public Assignment assignmentInfo(String stormId, Runnable callback);

    public VersionedData<Assignment> assignmentInfoWithVersion(String stormId, Runnable callback);

    public Integer assignmentVersion(String stormId, Runnable callback) throws Exception;

    public List<String> blobstoreInfo(String blobKey);

    public List<NimbusSummary> nimbuses();

    public void addNimbusHost(String nimbusId, NimbusSummary nimbusSummary);

    public List<String> activeStorms();

    /**
     * Get a storm base for a topology
     * @param stormId the id of the topology
     * @param callback something to call if the data changes (best effort)
     * @return the StormBase or null if it is not alive.
     */
    public StormBase stormBase(String stormId, Runnable callback);

    public ClusterWorkerHeartbeat getWorkerHeartbeat(String stormId, String node, Long port);

    public List<ProfileRequest> getWorkerProfileRequests(String stormId, NodeInfo nodeInfo);

    public List<ProfileRequest> getTopologyProfileRequests(String stormId);

    public void setWorkerProfileRequest(String stormId, ProfileRequest profileRequest);

    public void deleteTopologyProfileRequests(String stormId, ProfileRequest profileRequest);

    public Map<ExecutorInfo, ExecutorBeat> executorBeats(String stormId, Map<List<Long>, NodeInfo> executorNodePort);

    public List<String> supervisors(Runnable callback);

    public SupervisorInfo supervisorInfo(String supervisorId); // returns nil if doesn't exist

    public void setupHeatbeats(String stormId);

    public void teardownHeartbeats(String stormId);

    public void teardownTopologyErrors(String stormId);

    public List<String> heartbeatStorms();

    public List<String> errorTopologies();

    public List<String> backpressureTopologies();

    public void setTopologyLogConfig(String stormId, LogConfig logConfig);

    public LogConfig topologyLogConfig(String stormId, Runnable cb);

    public void workerHeartbeat(String stormId, String node, Long port, ClusterWorkerHeartbeat info);

    public void removeWorkerHeartbeat(String stormId, String node, Long port);

    public void supervisorHeartbeat(String supervisorId, SupervisorInfo info);

    public void workerBackpressure(String stormId, String node, Long port, long timestamp);

    public boolean topologyBackpressure(String stormId, long timeoutMs, Runnable callback);

    public void setupBackpressure(String stormId);

    public void removeBackpressure(String stormId);

    public void removeWorkerBackpressure(String stormId, String node, Long port);

    public void activateStorm(String stormId, StormBase stormBase);

    public void updateStorm(String stormId, StormBase newElems);

    public void removeStormBase(String stormId);

    public void setAssignment(String stormId, Assignment info);

    public void setupBlobstore(String key, NimbusInfo nimbusInfo, Integer versionInfo);

    public List<String> activeKeys();

    public List<String> blobstore(Runnable callback);

    public void removeStorm(String stormId);

    public void removeBlobstoreKey(String blobKey);

    public void removeKeyVersion(String blobKey);

    public void reportError(String stormId, String componentId, String node, Long port, Throwable error);

    public List<ErrorInfo> errors(String stormId, String componentId);

    public ErrorInfo lastError(String stormId, String componentId);

    public void setCredentials(String stormId, Credentials creds, Map<String, Object> topoConf) throws NoSuchAlgorithmException;

    public Credentials credentials(String stormId, Runnable callback);

    public void disconnect();

    /**
     * Get a private key used to validate a token is correct.
     * This is expected to be called from a privileged daemon, and the ACLs should be set up to only
     * allow nimbus and these privileged daemons access to these private keys.
     * @param type the type of service the key is for.
     * @param topologyId the topology id the key is for.
     * @param keyVersion the version of the key this is for.
     * @return the private key or null if it could not be found.
     */
    PrivateWorkerKey getPrivateWorkerKey(WorkerTokenServiceType type, String topologyId, long keyVersion);

    /**
     * Store a new version of a private key.
     * This is expected to only ever be called from nimbus.  All ACLs however need to be setup to allow
     * the given services access to the stored information.
     * @param type the type of service this key is for.
     * @param topologyId the topology this key is for
     * @param keyVersion the version of the key this is for.
     * @param key the key to store.
     */
    void addPrivateWorkerKey(WorkerTokenServiceType type, String topologyId, long keyVersion, PrivateWorkerKey key);

    /**
     * Get the next key version number that should be used for this topology id.
     * This is expected to only ever be called from nimbus, but it is acceptable if the ACLs are setup
     * so that it can work from a privileged daemon for the given service.
     * @param type the type of service this is for.
     * @param topologyId the topology id this is for.
     * @return the next version number.  It should be 0 for a new topology id/service combination.
     */
    long getNextPrivateWorkerKeyVersion(WorkerTokenServiceType type, String topologyId);

    /**
     * Remove all keys for the given topology that have expired. The number of keys should be small enough
     * that doing an exhaustive scan of them all is acceptable as there is no guarantee that expiration time
     * and version number are related.  This should be for all service types.
     * This is expected to only ever be called from nimbus and some ACLs may be setup so being called from other
     * daemons will cause it to fail.
     * @param topologyId the id of the topology to scan.
     */
    void removeExpiredPrivateWorkerKeys(String topologyId);

    /**
     * Remove all of the worker keys for a given topology.  Used to clean up after a topology finishes.
     * This is expected to only ever be called from nimbus and ideally should only ever work from nimbus.
     * @param topologyId the topology to clean up after.
     */
    void removeAllPrivateWorkerKeys(String topologyId);

    /**
     * Get a list of all topologyIds that currently have private worker keys stored, of any kind.
     * This is expected to only ever be called from nimbus.
     * @return the list of topology ids with any kind of private worker key stored.
     */
    Set<String> idsOfTopologiesWithPrivateWorkerKeys();

    /**
     * Get all of the supervisors with the ID as the key.
     */
    default Map<String, SupervisorInfo> allSupervisorInfo() {
        return allSupervisorInfo(null);
    }

    /**
     * @param callback be alerted if the list of supervisors change
     * @return All of the supervisors with the ID as the key
     */
    default Map<String, SupervisorInfo> allSupervisorInfo(Runnable callback) {
        Map<String, SupervisorInfo> ret = new HashMap<>();
        for (String id: supervisors(callback)) {
            ret.put(id, supervisorInfo(id));
        }
        return ret;
    }
    
    /**
     * Get a topology ID from the name of a topology
     * @param topologyName the name of the topology to look for
     * @return the id of the topology or null if it is not alive.
     */
    default Optional<String> getTopoId(final String topologyName) {
        String ret = null;
        for (String topoId: activeStorms()) {
            StormBase base = stormBase(topoId, null);
            if (base != null && topologyName.equals(base.get_name())) {
                ret = topoId;
                break;
            }
        }
        return Optional.ofNullable(ret);
    }
    
    default Map<String, Assignment> topologyAssignments() {
        Map<String, Assignment> ret = new HashMap<>();
        for (String topoId: assignments(null)) {
            ret.put(topoId, assignmentInfo(topoId, null));
        }
        return ret;
    }
    
    default Map<String, StormBase> topologyBases() {
        Map<String, StormBase> stormBases = new HashMap<>();
        for (String topologyId : activeStorms()) {
            StormBase base = stormBase(topologyId, null);
            if (base != null) { //rece condition with delete
                stormBases.put(topologyId, base);
            }
        }
        return stormBases;
    }
}
