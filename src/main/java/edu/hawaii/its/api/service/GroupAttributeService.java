package edu.hawaii.its.api.service;

import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetAttributeAssignmentsResults;

import java.util.List;
import java.util.Map;

public interface GroupAttributeService {

    public List<GroupingsServiceResult> changeOptInStatus(String groupingPath, String currentUsername, boolean isOptInOn);

    public List<GroupingsServiceResult> changeOptOutStatus(String groupingPath, String currentUsername, boolean isOptOutOn);

    public GroupingsServiceResult changeListservStatus(String groupingPath, String owenerUsername, boolean isListservOn);

    public GroupingsServiceResult changeReleasedGroupingStatus(String groupingPath, String ownerUsername, boolean isReleasedGroupingOn);

    public boolean isGroupAttribute(String groupPath, String attribute);

    public List<String> getAllSyncDestinations(String currentUsername);

    //do not include in REST controller
    public WsGetAttributeAssignmentsResults attributeAssignmentsResults(String assignType, String groupPath,
                                                                        String attributeName);

    public GroupingsServiceResult changeGroupAttributeStatus(String groupPath, String ownerUsername,
            String attributeName, boolean turnAttributeOn);

    public List<String> getAllSyncDestinations();

    public Map<String, Boolean> getSyncDestinations(String grouping);

    public GroupingsServiceResult updateDescription(String groupPath, String ownerUsername, String description);
}
