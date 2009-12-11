package eu.europeana.database.dao.fixture;

import eu.europeana.database.domain.*;
import eu.europeana.query.DocType;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Iterator;

/**
 * Tools to put some data into the database for testing
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 * @author Nicola Aloia <nicola.aloia@isti.cnr.it>
 */

public class DatabaseFixture {

    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional
    public List<User> createUsers(String who, int count) {
        List<User> users = new ArrayList<User>();
        for (int walk = 0; walk < count; walk++) {
            User user = new User(
                    null,
                    "user-" + who + walk,
                    who + walk + "@email.com",
                    "password-" + who + walk,
                    "First Name " + who + walk,
                    "Last Name " + who + walk,
                    "", "", "", false,
                    Role.ROLE_USER, true
            );
            entityManager.persist(user);
            users.add(user);
        }
        return users;
    }

    @Transactional
    public List<EuropeanaId> createEuropeanaIds(String collectionName, int count) {
        EuropeanaCollection collection = new EuropeanaCollection();
        collection.setName(collectionName);
        collection.setDescription("Created for testing");
        entityManager.persist(collection);
        List<EuropeanaId> ids = new ArrayList<EuropeanaId>();
        for (int walk = 0; walk < count; walk++) {
            EuropeanaId id = new EuropeanaId(collection);
            id.setCreated(new Date());
            id.setEuropeanaUri("http://europeana.uri.pretend/item" + walk);
            entityManager.persist(id);
            ids.add(id);
        }
        return ids;
    }

    @Transactional
    public <Entity> Entity fetch(Class<Entity> entityClass, Long id) {
        return entityManager.find(entityClass, id);
    }

    @Transactional
    public EuropeanaId fetchEuropeanaId(Long id) {
        EuropeanaId europeanaId = entityManager.find(EuropeanaId.class, id);
        europeanaId.getSocialTags().size();
        return europeanaId;
    }

    @Transactional
    public List<Partner> createPartners(String name, int count) {
        List<Partner> partners = new ArrayList<Partner>();
        for (int walk = 0; walk < count; walk++) {
            Partner partner = new Partner();
            partner.setName(name + walk);
            partner.setUrl("http://europeana.uri.pretend/item" + walk);
            partner.setSector(PartnerSector.RESEARCH_INSTITUTIONS);
            entityManager.persist(partner);
            partners.add(partner);
        }
        return partners;
    }

    @Transactional
    public List<Contributor> createContributors(String name, int count) {
        List<Contributor> contributors = new ArrayList<Contributor>();

        for (int walk = 0; walk < count; walk++) {
            Contributor contributor = new Contributor();
            contributor.setAcronym(name + walk);
            contributor.setCountry(Country.ITALY);
            contributor.setEnglishName(name + walk);
            contributor.setOriginalName(name + walk);
            contributor.setNumberOfPartners(String.valueOf(walk));
            contributor.setProviderId(name + walk);
            contributor.setUrl("http://europeana.uri.pretend/item" + walk);
            entityManager.persist(contributor);
            contributors.add(contributor);
        }
        return contributors;
    }

    @Transactional
    public List<SavedItem> createSavedItems(String name, int count, List<CarouselItem> carouselItems, List<EuropeanaId> europeanaIds, List<User> users) {
        List<SavedItem> savedItems = new ArrayList<SavedItem>();

        for (int walk = 0; walk < count; walk++) {
            SavedItem savedItem = new SavedItem();
            savedItem.setAuthor("Author " + name + walk);
            CarouselItem carouselItem = entityManager.find(CarouselItem.class, carouselItems.get(walk).getId());
            savedItem.setCarouselItem(carouselItem);
            savedItem.setDateSaved(new Date());
            savedItem.setDocType(DocType.IMAGE);
            savedItem.setEuropeanaId(europeanaIds.get(walk));
            savedItem.setEuropeanaObject("http://europeana.uri.pretend/item" + walk);
            savedItem.setTitle("Title " + name + walk);
            savedItem.setUser(users.get(walk));
            savedItem.setLanguage(Language.EN);
            entityManager.persist(savedItem);
            savedItems.add(savedItem);
        }
        return savedItems;
    }

    @Transactional
    public List<CarouselItem> createCarouselItems(String name, int count, List<EuropeanaId> europeanaIds) {
        List<CarouselItem> carouselItems = new ArrayList<CarouselItem>();

        for (int walk = 0; walk < count; walk++) {
            CarouselItem carouselItem = new CarouselItem();
            carouselItem.setCreator("Creator " + name + walk);
            carouselItem.setEuropeanaUri("http://europeana.uri.pretend/item" + walk);
            carouselItem.setEuropeanaId(europeanaIds.get(walk));
            carouselItem.setLanguage(Language.EN);
            carouselItem.setProvider("Provider " + name + walk);
            carouselItem.setTitle("Title " + name + walk);
            carouselItem.setThumbnail("thumbnail " + name + walk);
            carouselItem.setYear("2009");
            carouselItem.setType(DocType.IMAGE);
            // carouselItem.setSavedItem();

            entityManager.persist(carouselItem);
            carouselItems.add(carouselItem);
        }
        return carouselItems;
    }


    @Transactional
    public Partner getPartner(Long partnerId) {
        return entityManager.find(Partner.class, partnerId);
    }

    @Transactional
    public Contributor getContributor(Long contributorId) {
        return entityManager.find(Contributor.class, contributorId);
    }

    @Transactional
    public CarouselItem getCarouselItem(Long id) {
        return entityManager.find(CarouselItem.class, id);
    }


}
