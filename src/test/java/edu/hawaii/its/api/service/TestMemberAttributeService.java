package edu.hawaii.its.api.service;

import edu.internet2.middleware.grouperClient.ws.beans.WsGetAttributeAssignmentsResults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import edu.hawaii.its.api.type.Group;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.type.GroupingsServiceResultException;
import edu.hawaii.its.api.configuration.SpringBootWebApplication;
import edu.hawaii.its.api.type.Membership;
import edu.hawaii.its.api.type.Person;

import edu.internet2.middleware.grouperClient.ws.GcWebServiceError;
import edu.internet2.middleware.grouperClient.ws.beans.WsAttributeAssign;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembershipsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsMembership;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Null;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@ActiveProfiles("integrationTest")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SpringBootWebApplication.class })
public class TestMemberAttributeService {

    @Value("${groupings.api.test.grouping_many}")
    private String GROUPING;
    @Value("${groupings.api.test.grouping_many_basis}")
    private String GROUPING_BASIS;
    @Value("${groupings.api.test.grouping_many_include}")
    private String GROUPING_INCLUDE;
    @Value("${groupings.api.test.grouping_many_exclude}")
    private String GROUPING_EXCLUDE;
    @Value("${groupings.api.test.grouping_many_owners}")
    private String GROUPING_OWNERS;
    @Value("${groupings.api.opt_in}")
    private String OPT_IN;
    @Value("${groupings.api.assign_type_immediate_membership}")
    private String ASSIGN_TYPE_IMMEDIATE_MEMBERSHIP;
    @Value("${groupings.api.self_opted}")
    private String SELF_OPTED;

    public static final Log logger = LogFactory.getLog(MemberAttributeServiceImpl.class);

    // @Value("${groupings.api.test.grouping_many_apps}")
    // private String GROUPING_APPS;
    //groupings.api.test.grouping_many_apps = ${groupings.api.test.grouping_many}${groupings.api.apps}
    //
    // @Value("${groupings.api.test.grouping_many_admins}")
    // private String GROUPING_ADMINS;
    //groupings.api.test.grouping_many_admins = ${groupings.api.test.grouping_many}${groupings.api.admins}

    @Value("${groupings.api.success}")
    private String SUCCESS;

    @Value("${groupings.api.test.usernames}")
    private String[] username;

    @Value("${grouperClient.webService.login}")
    private String APP_USER;

    @Value("${groupings.api.test.admin_user}")
    private String ADMIN_USER;

    @Value("${groupings.api.failure}")
    private String FAILURE;

    @Value("${groupings.api.assign_type_group}")
    private String ASSIGN_TYPE_GROUP;

    @Value("${groupings.api.yyyymmddThhmm}")
    private String YYYYMMDDTHHMM;

    @Autowired
    GroupAttributeService groupAttributeService;

    @Autowired
    GroupingAssignmentService groupingAssignmentService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private HelperService helperService;

    @Autowired
    public Environment env; // Just for the settings check.

    @PostConstruct
    public void init() {
        Assert.hasLength(env.getProperty("grouperClient.webService.url"),
                "property 'grouperClient.webService.url' is required");
        Assert.hasLength(env.getProperty("grouperClient.webService.login"),
                "property 'grouperClient.webService.login' is required");
        Assert.hasLength(env.getProperty("grouperClient.webService.password"),
                "property 'grouperClient.webService.password' is required");
    }

    @Before
    public void setUp() {
        groupAttributeService.changeListservStatus(GROUPING, username[0], true);
        groupAttributeService.changeOptInStatus(GROUPING, username[0], true);
        groupAttributeService.changeOptOutStatus(GROUPING, username[0], true);

        //put in include
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[0]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[1]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[2]);

        //remove from exclude
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[4]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[5]);

        //add to exclude
        membershipService.deleteGroupingMemberByUsername(username[0], GROUPING, username[3]);

        //remove from owners
        memberAttributeService.removeOwnership(GROUPING, username[0], username[1]);

        // Remove from Exclude
        membershipService.addGroupMemberByUsername(username[0], GROUPING_INCLUDE, username[4]);

        // Turn off Self-Opted flags
        //todo Tests run properly without doing a isSelfOpted check on GROUPING_INCLUDE and username[1] for unknown reason
        membershipService.removeSelfOpted(GROUPING_INCLUDE, username[1]);
        if (memberAttributeService.isSelfOpted(GROUPING_EXCLUDE, username[4])) {
            membershipService.removeSelfOpted(GROUPING_EXCLUDE, username[4]);
        }
    }

    @Test
    public void isOwnerTest() {
        assertTrue(memberAttributeService.isOwner(GROUPING, username[0]));
    }

    @Test
    public void assignRemoveOwnershipTest() {
        //expect to fail
        GroupingsServiceResult assignOwnershipFail;
        GroupingsServiceResult removeOwnershipFail;

        assertTrue(memberAttributeService.isOwner(GROUPING, username[0]));
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));
        assertFalse(memberAttributeService.isOwner(GROUPING, username[2]));

        try {
            assignOwnershipFail = memberAttributeService.assignOwnership(GROUPING, username[1], username[1]);
        } catch (GroupingsServiceResultException gsre) {
            assignOwnershipFail = gsre.getGsr();
        }
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(assignOwnershipFail.getResultCode().startsWith(FAILURE));

        // get last modified time
        WsGetAttributeAssignmentsResults attributes =
                groupAttributeService.attributeAssignmentsResults(ASSIGN_TYPE_GROUP, GROUPING, YYYYMMDDTHHMM);
        String lastModTime1 = attributes.getWsAttributeAssigns()[0].getWsAttributeAssignValues()[0].getValueSystem();

        // get last modified time and make sure that it has changed
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException e) {
            fail();
        }

        GroupingsServiceResult assignOwnershipSuccess =
                memberAttributeService.assignOwnership(GROUPING, username[0], username[1]);
        assertTrue(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(assignOwnershipSuccess.getResultCode().startsWith(SUCCESS));

        attributes = groupAttributeService.attributeAssignmentsResults(ASSIGN_TYPE_GROUP, GROUPING, YYYYMMDDTHHMM);
        String lastModTime2 = attributes.getWsAttributeAssigns()[0].getWsAttributeAssignValues()[0].getValueSystem();
        assertNotEquals(lastModTime1, lastModTime2);

        try {
            removeOwnershipFail = memberAttributeService.removeOwnership(GROUPING, username[2], username[1]);
        } catch (GroupingsServiceResultException gsre) {
            removeOwnershipFail = gsre.getGsr();
        }

        assertTrue(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(removeOwnershipFail.getResultCode().startsWith(FAILURE));

        GroupingsServiceResult removeOwnershipSuccess =
                memberAttributeService.removeOwnership(GROUPING, username[0], username[1]);
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(removeOwnershipSuccess.getResultCode().startsWith(SUCCESS));

        //have an owner remove itself
        assignOwnershipSuccess = memberAttributeService.assignOwnership(GROUPING, username[0], username[1]);
        assertTrue(memberAttributeService.isOwner(GROUPING, username[1]));
        removeOwnershipSuccess = memberAttributeService.removeOwnership(GROUPING, username[1], username[1]);
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));

    }

    @Test
    public void isMemberTest() {
        //test isMember with username
        memberAttributeService.isMember(GROUPING, "zknoebel");

        assertTrue(memberAttributeService.isMember(GROUPING_INCLUDE, username[1]));
        assertFalse(memberAttributeService.isMember(GROUPING_INCLUDE, username[3]));

        assertTrue(memberAttributeService.isMember(GROUPING_EXCLUDE, username[3]));
        assertFalse(memberAttributeService.isMember(GROUPING_EXCLUDE, username[1]));

        //test isMember with Person
        Person testPersonInclude = new Person("tst01name", "iamtst01", username[1]);
        Person testPersonExclude = new Person("tst03name", "iamtst03", username[3]);
        Person testPersonNull = null;

        assertTrue(memberAttributeService.isMember(GROUPING_INCLUDE, testPersonInclude));
        assertFalse(memberAttributeService.isMember(GROUPING_INCLUDE, testPersonExclude));

        assertTrue(memberAttributeService.isMember(GROUPING_EXCLUDE, testPersonExclude));
        assertFalse(memberAttributeService.isMember(GROUPING_EXCLUDE, testPersonInclude));

        // Test if username does not exist
        try {
            assertFalse(memberAttributeService.isMember(GROUPING_INCLUDE, "someName"));
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        // Test if username/person is NULL
        try {
            assertFalse(memberAttributeService.isMember(GROUPING_INCLUDE, testPersonNull));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        String nullString = null;
        try {
            assertFalse(memberAttributeService.isMember(GROUPING_INCLUDE, nullString));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        // Test if grouping does not exist
        try {
            assertFalse(memberAttributeService.isMember("someGroup", username[1]));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        try {
            assertFalse(memberAttributeService.isMember("someGroup", testPersonExclude));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        // Test if grouping is NULL
        try {
            assertFalse(memberAttributeService.isSelfOpted(null, username[1]));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        try {
            assertFalse(memberAttributeService.isMember(null, testPersonExclude));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

    @Test
    public void isSelfOptedTest() {
        //todo How to change/know if user is SelfOpted/Admin/Appuser etc.

        // User is not self opted because user is not in group
        assertFalse(memberAttributeService.isSelfOpted(GROUPING_EXCLUDE, username[4]));
        membershipService.addGroupMemberByUsername(username[0], GROUPING_EXCLUDE, username[4]);

        // User is not self opted b/c added by owner
        assertFalse(memberAttributeService.isSelfOpted(GROUPING_EXCLUDE, username[4]));

        membershipService.addSelfOpted(GROUPING_EXCLUDE, username[4]);

        // Alternate implementation
        //membershipService.deleteGroupMemberByUsername(username[0], GROUPING_EXCLUDE, username[4]);
        //membershipService.optOut(username[4], GROUPING);

        // User is self opted b/c added himself
        assertTrue(memberAttributeService.isSelfOpted(GROUPING_EXCLUDE, username[4]));

        // User does not exist
        try {
            assertFalse(memberAttributeService.isSelfOpted(GROUPING_EXCLUDE, "someName"));
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        // User is null
        try {
            assertFalse(memberAttributeService.isSelfOpted(GROUPING_EXCLUDE, null));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        // Group does not exist
        try {
            assertFalse(memberAttributeService.isSelfOpted("someGroup", username[4]));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        // Group path is null
        try {
            assertFalse(memberAttributeService.isSelfOpted(null, username[4]));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

    @Test
    public void isAppTest() {
        //todo Write in overrides file who the App User is (not directly for security reasons)
        //todo I need permissions so I know who App User and Admin User are on Grouper Test Server

        // User is not app user
        assertFalse(memberAttributeService.isApp(username[1]));

        // User is app user
        assertTrue(memberAttributeService.isApp(APP_USER));

        // User does not exist
        try {
            assertFalse(memberAttributeService.isApp("someName"));
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        // User is null
        try {
            assertFalse(memberAttributeService.isApp(null));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

    @Test
    public void isSuperUserTest() {
        //todo

        // User is not super user
        assertFalse(memberAttributeService.isSuperuser(username[1]));

        // User is super user
        assertTrue(memberAttributeService.isSuperuser(APP_USER));
        assertTrue(memberAttributeService.isSuperuser(ADMIN_USER));

        // User does not exist
        try {
            assertFalse(memberAttributeService.isSuperuser("someName"));
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        // User is null
        try {
            assertFalse(memberAttributeService.isSuperuser(null));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

    @Test
    public void isAdminTest() {
        //todo

        // User is not admin
        assertFalse(memberAttributeService.isAdmin(username[1]));

        // User is admin
        assertTrue(memberAttributeService.isAdmin(ADMIN_USER));

        // User does not exist
        try {
            assertFalse(memberAttributeService.isAdmin("someName"));
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        // User is null
        try {
            assertFalse(memberAttributeService.isAdmin(null));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

    @Test
    public void getMembershipAttributesTest() {
        //todo I don't know what to put as arguments (particularly membershipid)
        // Ternary Operator (for reference)
        // if(!null) return wsAttributes
        // else return grouperFS.makeEmptyWSAttributeAssignArray

        String type = ASSIGN_TYPE_IMMEDIATE_MEMBERSHIP;
        String uuid = SELF_OPTED;

        WsGetMembershipsResults results = helperService.membershipsResults(username[1], GROUPING_INCLUDE);
        String membershipID = helperService.extractFirstMembershipID(results);
        assertEquals(0, memberAttributeService.getMembershipAttributes(type, uuid, membershipID).length);

        membershipService.addSelfOpted(GROUPING_INCLUDE, username[1]);

        //WsMembership membership = new WsMembership();
        //membershipID = membership.getMembershipId();
        //uuid = membership.getOwnerNameOfAttributeDef();
        //membership.setOwnerStemId(OPT_IN);
        //membership.setMembershipId(membershipID);

        WsAttributeAssign[] assigns = memberAttributeService.getMembershipAttributes(type, uuid, membershipID);
        //logger.info("Assigns length is " + assigns.length + ";");
        assertTrue(assigns.length == 1);

        // Test with invalid fields
        try {
            assigns = memberAttributeService.getMembershipAttributes("type", "uuid", "memberid");
            assertTrue(assigns.length == 0);
        } catch (RuntimeException re) {
            re.printStackTrace();
        }

        // Test with null fields
        try {
            assigns = memberAttributeService.getMembershipAttributes(null, null, null);
            assertTrue(assigns.length == 0);
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

    @Test
    public void getUserAttributesTest() {

        // Base test
        String useruid = username[1];

        Map<String, String> attributes = memberAttributeService.getUserAttributes(ADMIN_USER, useruid);
        assertTrue(attributes.get("uid").equals("iamtst02"));
        assertTrue(attributes.get("cn").equals("tst02name"));
        assertTrue(attributes.get("sn").equals("tst02name"));
        assertTrue(attributes.get("givenName").equals("tst02name"));
        assertTrue(attributes.get("uhuuid").equals("iamtst02"));

        //todo Owner test
        attributes = memberAttributeService.getUserAttributes("iamtst01", useruid);
        assertTrue(attributes.get("uid").equals("iamtst02"));
        assertTrue(attributes.get("cn").equals("tst02name"));
        assertTrue(attributes.get("sn").equals("tst02name"));
        assertTrue(attributes.get("givenName").equals("tst02name"));
        assertTrue(attributes.get("uhuuid").equals("iamtst02"));

        //todo Not an owner test
        attributes = memberAttributeService.getUserAttributes("iamtst03", useruid);
        assertTrue(attributes.get("uid").equals(""));
        assertTrue(attributes.get("cn").equals(""));
        assertTrue(attributes.get("sn").equals(""));
        assertTrue(attributes.get("givenName").equals(""));
        assertTrue(attributes.get("uhuuid").equals(""));

        //todo Implement assertThat over assertTrue/assertEquals/etc.
        //        assertEquals("iamtst02", attributes.get("uhuuid"));
        //        assertThat(attributes.get("uhuuid"), equalTo("iamtst02"));

        // Test with invalid username
        try {
            attributes = memberAttributeService.getUserAttributes(ADMIN_USER, "notarealperson");
            fail("Shouldn't be here.");
        } catch (GcWebServiceError gce) {
            gce.printStackTrace();
        }

        // Test with null field
        try {
            attributes = memberAttributeService.getUserAttributes(ADMIN_USER, null);
            fail("Shouldn't be here.");
        } catch (GcWebServiceError gce) {
            gce.printStackTrace();
        }
    }

    @Test
    public void getMembersTest() {

        // iamtst04 is in the basis group
        List<Person> members = memberAttributeService.getMembers(GROUPING_BASIS, username[3]);
        assertThat(members.get(0).getName(), equalTo("tst04name"));
        assertThat(members.get(0).getUsername(), equalTo("iamtst04"));
        assertThat(members.get(0).getUuid(), equalTo("iamtst04"));

        // iamtst01 is not in the basis group (results list should be empty)
        members = memberAttributeService.getMembers(GROUPING_BASIS, username[0]);
        assertThat(members.size(), equalTo(0));
    }
}
