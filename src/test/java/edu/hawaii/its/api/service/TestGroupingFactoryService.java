package edu.hawaii.its.api.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import edu.hawaii.its.api.configuration.SpringBootWebApplication;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.type.GroupingsServiceResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


import edu.hawaii.its.api.type.Group;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.type.Person;

import edu.internet2.middleware.grouperClient.api.GcGroupSave;
import edu.internet2.middleware.grouperClient.ws.StemScope;
import edu.internet2.middleware.grouperClient.ws.beans.WsFindGroupsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetGroupsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGroup;
import edu.internet2.middleware.grouperClient.ws.beans.WsGroupLookup;
import edu.internet2.middleware.grouperClient.ws.beans.WsStemLookup;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubjectLookup;

import edu.internet2.middleware.grouperClient.ws.beans.WsGroupToSave;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

@ActiveProfiles("integrationTest")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SpringBootWebApplication.class })
public class TestGroupingFactoryService {

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


    @Value("${groupings.api.test.grouping_new}")
    private String GROUPING_NEW;
    @Value("${groupings.api.test.grouping_new_basis}")
    private String GROUPING_NEW_BASIS;
    @Value("${groupings.api.test.grouping_new_include}")
    private String GROUPING_NEW_INCLUDE;
    @Value("${groupings.api.test.grouping_new_exclude}")
    private String GROUPING_NEW_EXCLUDE;
    @Value("${groupings.api.test.grouping_new_owners}")
    private String GROUPING_NEW_OWNERS;

    @Value("${groupings.api.test.grouping_temp_test}")
    private String TEMP_TEST;

    @Value("${groupings.api.success}")
    private String SUCCESS;

    @Value("${groupings.api.failure}")
    private String FAILURE;

    @Value("${groupings.api.test.usernames}")
    private String[] username;

    @Value("${grouperClient.webService.login}")
    private String APP_USER;

    private String apple = "apple";

    @Autowired
    GroupAttributeService groupAttributeService;

    @Autowired
    GroupingAssignmentService groupingAssignmentService;

    @Autowired
    private GrouperFactoryService grouperFactoryService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired
    private GroupingFactoryService groupingFactoryService;

    @Autowired
    private MembershipService membershipService;

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
        // Make sure the grouping folder is cleared
        if(!groupingFactoryService.pathIsEmpty(APP_USER, TEMP_TEST)){
            groupingFactoryService.deleteGrouping(APP_USER, TEMP_TEST);
        }
    }

    @Test
    public void constructorTest() {
        assertNotNull(groupingFactoryService);
    }

    // todo running this with out a stem gives an error "Cant find stem: ..." - make sure the code adds this stem if
    // necessary
    // todo the code should give admin privileges to the groupingSuperusers group
    @Test
    public void addGroupingTest() {

        List<GroupingsServiceResult> results = new ArrayList<>();
        GroupingsServiceResult sResults;



        //Works correctly
        assertThat(memberAttributeService.isSuperuser(APP_USER), equalTo(true));

        results = groupingFactoryService.addGrouping(APP_USER, TEMP_TEST);

        assertThat(groupingFactoryService.pathIsEmpty(APP_USER, TEMP_TEST),
                equalTo(false));


        //Fails when the grouping already exists
        try {
            results = groupingFactoryService.addGrouping(APP_USER, TEMP_TEST);

        } catch (GroupingsServiceResultException gsre) {

            sResults = gsre.getGsr();
            assertThat(sResults.getResultCode(), startsWith(FAILURE));
        }

        //delete the grouping
        groupingFactoryService.deleteGrouping(APP_USER, TEMP_TEST);



        //Fails when user trying to add grouping is not admin
        try {

            results = groupingFactoryService.addGrouping("sbraun", TEMP_TEST + ":kahlin-test");

        } catch (GroupingsServiceResultException gsre) {

            sResults = gsre.getGsr();
            assertThat(sResults.getResultCode(), startsWith(FAILURE));
        }





    }
    @Test
    public void deleteGroupingTest() {

        List<GroupingsServiceResult> results = new ArrayList<>();
        GroupingsServiceResult sResults;

        //add the grouping
        groupingFactoryService.addGrouping(APP_USER, TEMP_TEST);

        //Works correctly
        assertThat(memberAttributeService.isSuperuser(APP_USER), equalTo(true));

        results = groupingFactoryService.deleteGrouping(APP_USER, TEMP_TEST);

        assertThat(groupingFactoryService.pathIsEmpty(APP_USER, TEMP_TEST),
                equalTo(true));


        //Fails when the grouping doesn't exists
        try {
            results = groupingFactoryService.deleteGrouping(APP_USER, TEMP_TEST);

        } catch (GroupingsServiceResultException gsre) {

            sResults = gsre.getGsr();
            assertThat(sResults.getResultCode(), startsWith(FAILURE));
        }



        //Fails when user trying to delete grouping is not admin
        try {

            results = groupingFactoryService.deleteGrouping("sbraun", TEMP_TEST + ":kahlin-test");

        } catch (GroupingsServiceResultException gsre) {

            sResults = gsre.getGsr();
            assertThat(sResults.getResultCode(), startsWith(FAILURE));
        }
    }




    // Methods below are for testing, to be deleted before pushing to master.
    @Test
    public void simpleTest() {
        assertThat(apple, equalTo("apple"));
    }

    @Test
    public void descriptionTest() {
        WsFindGroupsResults clintGroupResult = grouperFactoryService.makeWsFindGroupsResults("hawaii.edu:custom:test:clintmor:clintmor-test");

        // We only need the uuid from the (to be updated) group below (see if actual methods can
        // just grab the uuid without creating this group instance...)
        WsGroup clintGroup = clintGroupResult.getGroupResults()[0];

        // To save an updated Group in Grouper, must do this...
        WsGroup clint2 = new WsGroup();
        clint2.setDescription("TEST TEST TEST Test grouping for clintmor");
        clint2.setDisplayExtension("clintmor-test");
        clint2.setName("hawaii.edu:custom:test:clintmor:clintmor-test");
        clint2.setExtension("clintmor-test");

        WsGroupToSave updatedGroup = new WsGroupToSave();
        updatedGroup.setWsGroup(clint2);

        WsGroupLookup groupLookup = new WsGroupLookup("hawaii.edu:custom:test:clintmor:clintmor-test", clintGroup.getUuid());
        updatedGroup.setWsGroupLookup(groupLookup);

        new GcGroupSave().addGroupToSave(updatedGroup).execute();
    }

    @Test
    public void descriptionTest2() {
        WsGroup clint3 = new WsGroup();
        clint3.setDescription("Description Test 2: Test grouping for clintmor");

        WsGroupLookup wsGL = new WsGroupLookup("hawaii.edu:custom:test:clintmor:clintmor-test",
                grouperFactoryService.makeWsFindGroupsResults("hawaii.edu:custom:test:clintmor:clintmor-test")
                .getGroupResults()[0].getUuid());

        WsGroupToSave updatedGroup = new WsGroupToSave();
        updatedGroup.setWsGroup(clint3);
        updatedGroup.setWsGroupLookup(wsGL);

        new GcGroupSave().addGroupToSave(updatedGroup).execute();
    }

    @Test
    public void updateGroup2Test() {
        String testString = "integration test Description";
        String testPath = "hawaii.edu:custom:test:clintmor:clintmor-test";

//        try {
//
//        } catch (GroupingsServiceResultException e) {
//
//        }

        grouperFactoryService.updateGroup2(testPath, testString);
        assertThat(grouperFactoryService.makeWsFindGroupsResults(testPath).getGroupResults()[0].getDescription(), equalTo(testString));
    }
}
