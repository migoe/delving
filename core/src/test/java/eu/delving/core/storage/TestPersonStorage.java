/*
 * Copyright 2010 DELVING BV
 *
 *  Licensed under the EUPL, Version 1.0 or? as soon they
 *  will be approved by the European Commission - subsequent
 *  versions of the EUPL (the "Licence");
 *  you may not use this work except in compliance with the
 *  Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  http://ec.europa.eu/idabc/eupl
 *
 *  Unless required by applicable law or agreed to in
 *  writing, software distributed under the Licence is
 *  distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied.
 *  See the Licence for the specific language governing
 *  permissions and limitations under the Licence.
 */

package eu.delving.core.storage;

import com.mongodb.Mongo;
import eu.delving.domain.Language;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.List;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "/core-application-context.xml"
})

public class TestPersonStorage {
    private static final String TEST_DB_NAME = "test-person";

    @Autowired
    private Mongo mongo;

    @Autowired
    private UserRepoImpl personStorage;
    public static final String EMAIL = "dude@delving.eu";

    @Before
    public void before() {
        personStorage.setDatabaseName(TEST_DB_NAME);
        mongo.dropDatabase(TEST_DB_NAME);
        UserRepo.Person person = personStorage.createPerson(EMAIL);
        person.setEnabled(true);
        person.setFirstName("Joe");
        person.setPassword("gumby");
        person.setLastName("Dude");
        person.setLastLogin(new Date());
        person.save();
    }

    @After
    public void after() {
        mongo.dropDatabase(TEST_DB_NAME);
    }

    @Test
    public void authenticate() {
        UserRepo.Person dude = personStorage.authenticate(EMAIL, "gumbi");
        Assert.assertNull(dude);
        dude = personStorage.authenticate(EMAIL, "gumby");
        Assert.assertNotNull(dude);
    }

    @Test
    public void changeField() {
        UserRepo.Person dude = personStorage.byEmail(EMAIL);
        dude.setFirstName("Mary");
        dude.save();
        dude = personStorage.byEmail(EMAIL);
        Assert.assertEquals("New name didn't hold", "Mary", dude.getFirstName());
    }

    @Test
    public void addRemoveItem() {
        UserRepo.Person dude = personStorage.byEmail(EMAIL);
        dude.addItem("Author", "Title", Language.NO);
        dude.save();
        dude = personStorage.byEmail(EMAIL);
        List<UserRepo.Item> items = dude.getItems();
        Assert.assertEquals("Should be one item", 1, items.size());
        UserRepo.Item item = items.get(0);
        Assert.assertEquals("field wrong", "Author", item.getAuthor());
        Assert.assertEquals("field wrong", "Title", item.getTitle());
        Assert.assertEquals("field wrong", Language.NO, item.getLanguage());
        item.remove();
        dude.save();
        dude = personStorage.byEmail(EMAIL);
        Assert.assertEquals("Should be empty", 0, dude.getItems().size());
    }

    @Test
    public void addRemoveSearch() {
        UserRepo.Person dude = personStorage.byEmail(EMAIL);
        dude.addSearch("Query", "QueryString", Language.FI);
        dude.save();
        dude = personStorage.byEmail(EMAIL);
        List<UserRepo.Search> searches = dude.getSearches();
        Assert.assertEquals("Should be one item", 1, searches.size());
        UserRepo.Search search = searches.get(0);
        Assert.assertEquals("field wrong", "Query", search.getQuery());
        Assert.assertEquals("field wrong", "QueryString", search.getQueryString());
        Assert.assertEquals("field wrong", Language.FI, search.getLanguage());
        search.remove();
        dude.save();
        dude = personStorage.byEmail(EMAIL);
        Assert.assertEquals("Should be empty", 0, dude.getSearches().size());
    }
}
