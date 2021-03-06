/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.startup;

import com.google.common.base.Preconditions;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;
import stroom.index.shared.IndexService;
import stroom.resources.query.v1.StroomIndexQueryResource;
import stroom.resources.NamedResource;
import stroom.resources.query.v1.SqlStatisticsQueryResource;
import stroom.resources.authentication.v1.AuthenticationResource;
import stroom.resources.authorisation.v1.AuthorisationResource;
import stroom.search.server.SearchResultCreatorManager;
import stroom.security.server.AuthenticationService;
import stroom.security.server.AuthorisationService;
import stroom.security.server.JWTService;
import stroom.statistics.server.sql.StatisticsQueryService;
import stroom.util.upgrade.UpgradeDispatcherServlet;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

public class Resources {

    private final StroomIndexQueryResource stroomIndexQueryResource;
    private final SqlStatisticsQueryResource sqlStatisticsQueryResource;
    private final AuthenticationResource authenticationResource;
    private final AuthorisationResource authorisationResource;
    private final List<NamedResource> resources = new ArrayList<>();

    public Resources(JerseyEnvironment jersey, ServletMonitor servletMonitor) {

        stroomIndexQueryResource = new StroomIndexQueryResource();
        registerResource(jersey, stroomIndexQueryResource);

        sqlStatisticsQueryResource = new SqlStatisticsQueryResource();
        registerResource(jersey, sqlStatisticsQueryResource);

        authenticationResource = new AuthenticationResource();
        registerResource(jersey, authenticationResource);

        authorisationResource = new AuthorisationResource();
        registerResource(jersey, authenticationResource);

        servletMonitor.registerApplicationContextListener(this::configureLuceneQueryResource);
        servletMonitor.registerApplicationContextListener(this::configureSqlStatisticsQueryResource);
        servletMonitor.registerApplicationContextListener(this::configureAuthenticationResource);
        servletMonitor.registerApplicationContextListener(this::configureAuthorisationResource);
    }

    private void registerResource(JerseyEnvironment jersey, Object resource) {
        jersey.register(Preconditions.checkNotNull(resource));
        if (resource instanceof NamedResource) {
            resources.add((NamedResource) resource);
        }
    }

    public void register(ServletHolder upgradeDispatcherServletHolder) {

        boolean apisAreNotYetConfigured = true;
        while (apisAreNotYetConfigured) {
            try {
                // This checks to see if the servlet has started. It'll throw an exception if it has.
                // I don't know of another way to check to see if it's ready.
                // If we try and get the servlet manually it'll fail to initialise because it won't have the ServletContext;
                // i.e. we need to let the Spring lifecycle stuff take care of this for us.
                upgradeDispatcherServletHolder.ensureInstance();

                UpgradeDispatcherServlet servlet = (UpgradeDispatcherServlet) upgradeDispatcherServletHolder.getServlet();
                ApplicationContext applicationContext = servlet.getWebApplicationContext();

                if (applicationContext != null) {
                    configureLuceneQueryResource(applicationContext);
                    configureSqlStatisticsQueryResource(applicationContext);
                    configureAuthenticationResource(applicationContext);
                    configureAuthorisationResource(applicationContext);
                    apisAreNotYetConfigured = false;
                }
            } catch (ServletException e) {
                // This could be an UnavailableException, caused by ensureInstance().
                // We don't care, we're going to keep trying.
            }
        }
    }

    public List<NamedResource> getResources(){
        return resources;
    }

    private void configureLuceneQueryResource(ApplicationContext applicationContext){
        SearchResultCreatorManager searchResultCreatorManager = applicationContext.getBean(SearchResultCreatorManager.class);
        IndexService indexService = applicationContext.getBean(IndexService.class);
        stroomIndexQueryResource.setIndexService(indexService);
        stroomIndexQueryResource.setSearchResultCreatorManager(searchResultCreatorManager);
    }

    private void configureSqlStatisticsQueryResource(ApplicationContext applicationContext){
        StatisticsQueryService statisticsQueryService = applicationContext.getBean(StatisticsQueryService.class);
        sqlStatisticsQueryResource.setStatisticsQueryService(statisticsQueryService);
    }

    private void configureAuthenticationResource(ApplicationContext applicationContext){
        AuthenticationService authenticationService = applicationContext.getBean(AuthenticationService.class);
        JWTService jwtService = applicationContext.getBean(JWTService.class);
        authenticationResource.setAuthenticationService(authenticationService);
        authenticationResource.setJwtService(jwtService);
    }

    private void configureAuthorisationResource(ApplicationContext applicationContext){
        AuthorisationService authorisationService = applicationContext.getBean(AuthorisationService.class);
        authorisationResource.setAuthorisationService(authorisationService);
    }
}