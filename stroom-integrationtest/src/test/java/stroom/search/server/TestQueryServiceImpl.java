/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.search.server;

import stroom.AbstractCoreIntegrationTest;
import stroom.dashboard.shared.Dashboard;
import stroom.dashboard.shared.DashboardService;
import stroom.dashboard.shared.FindQueryCriteria;
import stroom.dashboard.shared.Query;
import stroom.dashboard.shared.QueryService;
import stroom.entity.server.util.BaseEntityDeProxyProcessor;
import stroom.entity.shared.BaseCriteria.OrderByDirection;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.FolderService;
import stroom.index.shared.Index;
import stroom.index.shared.IndexService;
import stroom.query.shared.ExpressionOperator;
import stroom.query.shared.ExpressionOperator.Op;
import stroom.query.shared.ExpressionTerm;
import stroom.query.shared.QueryData;
import stroom.security.shared.User;
import stroom.security.shared.UserService;
import stroom.util.thread.ThreadUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;

public class TestQueryServiceImpl extends AbstractCoreIntegrationTest {
    @Resource
    private DashboardService dashboardService;
    @Resource
    private QueryService queryService;
    @Resource
    private IndexService indexService;
    @Resource
    private UserService userService;
    @Resource
    private FolderService folderService;

    private Dashboard dashboard;
    private User user;
    private Query testQuery;
    private Query refQuery;

    @Override
    protected void onBefore() {
        // need an explicit teardown and setup of the DB before each test method
        clean();

        user = userService.createUser("testuser");

        final DocRef testFolder = DocRef.create(folderService.create(null, "Test Folder"));

        dashboard = dashboardService.create(testFolder, "Test");

        final Index index = indexService.create(testFolder, "Test index");
        final DocRef dataSourceRef = DocRef.create(index);

        refQuery = queryService.create(null, "Ref query");
        refQuery.setDashboard(dashboard);
        final QueryData refQueryData = new QueryData();
        refQuery.setQueryData(refQueryData);
        refQueryData.setDataSource(dataSourceRef);
        refQueryData.setExpression(new ExpressionOperator());
        queryService.save(refQuery);

        // Ensure the two query creation times are separated by one second so that ordering by time works correctly in
        // the test.
        ThreadUtil.sleep(1000);

        testQuery = queryService.create(null, "Test query");
        testQuery.setDashboard(dashboard);
        final QueryData testQueryData = new QueryData();
        testQuery.setQueryData(testQueryData);
        testQueryData.setDataSource(dataSourceRef);

        final ExpressionOperator root = new ExpressionOperator();
        root.setType(Op.OR);

        final ExpressionTerm content = new ExpressionTerm();
        content.setField("Some field");
        content.setValue("Some value");

        root.addChild(content);

        testQueryData.setExpression(root);
        testQuery = queryService.save(testQuery);
    }

    @Test
    public void testQueryRetrieval() {
        final FindQueryCriteria criteria = new FindQueryCriteria();
        criteria.obtainDashboardIdSet().add(dashboard.getId());
        criteria.setOrderBy(FindQueryCriteria.ORDER_BY_TIME, OrderByDirection.DESCENDING);

        final BaseResultList<Query> list = queryService.find(criteria);

        Assert.assertEquals(2, list.size());

        final Query query = list.get(0);

        Assert.assertNotNull(query);
        Assert.assertEquals("Test query", query.getName());
        Assert.assertNotNull(query.getData());

        final ExpressionOperator root = query.getQueryData().getExpression();

        Assert.assertEquals(1, root.getChildren().size());

        final StringBuilder sb = new StringBuilder();
        sb.append("<expression>\n");
        sb.append("    <enabled>true</enabled>\n");
        sb.append("    <op>OR</op>\n");
        sb.append("    <children>\n");
        sb.append("        <term>\n");
        sb.append("            <enabled>true</enabled>\n");
        sb.append("            <field>Some field</field>\n");
        sb.append("            <condition>CONTAINS</condition>\n");
        sb.append("            <value>Some value</value>\n");
        sb.append("        </term>\n");
        sb.append("    </children>\n");
        sb.append("</expression>\n");

        String actual = query.getData();
        actual = actual.replaceAll("\\s*", "");
        String expected = sb.toString();
        expected = expected.replaceAll("\\s*", "");
        Assert.assertTrue(actual.contains(expected));
    }

    @Test
    public void testLoad() {
        Query query = new Query();
        query.setId(testQuery.getId());
        query = queryService.load(query);

        Assert.assertNotNull(query);
        Assert.assertEquals("Test query", query.getName());
        Assert.assertNotNull(query.getData());
        final ExpressionOperator root = query.getQueryData().getExpression();
        Assert.assertEquals(1, root.getChildren().size());
    }

//    @Test
//    public void testLoadById() {
//        final Query query = queryService.loadById(testQuery.getId());
//
//        Assert.assertNotNull(query);
//        Assert.assertEquals("Test query", query.getName());
//        Assert.assertNotNull(query.getData());
//        final ExpressionOperator root = query.getQueryData().getExpression();
//        Assert.assertEquals(1, root.getChildren().size());
//    }

    @Test
    public void testClientSideStuff1() {
        Query query = queryService.loadByUuid(refQuery.getUuid());
        query = ((Query) new BaseEntityDeProxyProcessor(true).process(query));
        queryService.save(query);
    }

    @Test
    public void testClientSideStuff2() {
        Query query = queryService.loadByUuid(testQuery.getUuid());
        query = ((Query) new BaseEntityDeProxyProcessor(true).process(query));
        queryService.save(query);
    }

    @Test
    public void testDeleteKids() {
        Query query = queryService.loadByUuid(testQuery.getUuid());
        ExpressionOperator root = query.getQueryData().getExpression();
        root.getChildren().remove(0);
        queryService.save(query);

        query = queryService.loadByUuid(testQuery.getUuid());

        Assert.assertEquals("Test query", query.getName());
        root = query.getQueryData().getExpression();
        Assert.assertEquals(0, root.getChildren().size());
    }
}
