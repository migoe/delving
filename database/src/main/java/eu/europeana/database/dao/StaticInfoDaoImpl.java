/*
 * Copyright 2007 EDL FOUNDATION
 *
 * Licensed under the EUPL, Version 1.1 or as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.europeana.database.dao;

import eu.europeana.database.StaticInfoDao;
import eu.europeana.database.domain.*;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import eu.europeana.database.DashboardDao;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 * @author Nicola Aloia
 */
public class StaticInfoDaoImpl implements StaticInfoDao {
    private Logger log = Logger.getLogger(getClass());
    private DashboardDao dashBoardDao;

    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional
    @SuppressWarnings("unchecked")
    public List<Partner> getAllPartnerItems() {
        Query q = entityManager.createQuery("select pi from Partner pi order by pi.sector");
        return (List<Partner>) q.getResultList();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<Contributor> getAllContributorItems() {
        Query q = entityManager.createQuery("select con from Contributor con order by con.country");
        return (List<Contributor>) q.getResultList();
    }
    /* this or the following?
@Transactional
public void saveContributor(Contributor contributorX) {
   Query query = entityManager.createQuery("select co from Contributor as co where co.providerId = :providerId");
   query.setParameter("providerId", contributorX.getProviderId());
   Contributor contributor = null;
   try {
       contributor = (Contributor) query.getSingleResult();
       contributor.setProviderId(contributorX.getProviderId());
       contributor.setOriginalName(contributorX.getOriginalName());
       contributor.setEnglishName(contributorX.getEnglishName());
       contributor.setAcronym(contributorX.getAcronym());
       contributor.setCountry(contributorX.getCountry());
       contributor.setNumberOfPartners(contributorX.getNumberOfPartners());
       contributor.setUrl(contributorX.getUrl());
   } catch (NoResultException e) {
       if (contributorX.getProviderId() != null) {
           entityManager.persist(contributorX);
       }
   }
}     */

    @Transactional
    public Contributor saveContributor(Contributor contributor) {
        return entityManager.merge(contributor);
    }

    /*
@Transactional
public void savePartner(Partner partnerX) {
Query query = entityManager.createQuery("select po from Partner as po where po.name = :name");
query.setParameter("name", partnerX.getName());
Partner partner = null;
try {
  partner = (Partner) query.getSingleResult();
  partner.setName(partnerX.getName());
  partner.setUrl(partnerX.getUrl());
  partnerX.setSector(partnerX.getSector());
} catch (Exception e) {
  if (partnerX.getName() != null) {
      entityManager.persist(partnerX);
  }
}
}          */
    @Transactional
    public Partner savePartner(Partner partner) {
        return entityManager.merge(partner);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<StaticPage> getAllStaticPages() {
        Query query = entityManager.createQuery("select sp from StaticPage as sp");
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<MessageKey> getAllTranslationMessages() {
        Query query = entityManager.createQuery("select trans from Translation as trans");
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public StaticPage fetchStaticPage(Language language, String pageName) {
        Query query = entityManager.createQuery("select sp from StaticPage as sp where sp.language = :language and sp.pageType = :pageType");
        query.setParameter("language", language);
        query.setParameter("pageType", StaticPageType.get(pageName));
        List results = query.getResultList();
        if (results.size() == 0) {
            query.setParameter("language", Language.EN);
            results = query.getResultList();
            if (results.size() == 0) {
                return new StaticPage(StaticPageType.get(pageName), language);
            }
        }
        return (StaticPage) results.get(0);
    }

    @Transactional
    public StaticPage fetchStaticPage(StaticPageType pageType, Language language) {
        Query query = entityManager.createQuery("select sp from StaticPage sp where sp.pageType = :pageType and sp.language = :language");
        query.setParameter("pageType", pageType);
        query.setParameter("language", language);
        try {
            return (StaticPage) query.getSingleResult();
        }
        catch (NoResultException e) {
            StaticPage page = new StaticPage(pageType, language);
            entityManager.persist(page);
            return page;
        }
    }

    @Transactional
    public void setStaticPage(StaticPageType pageType, Language language, String content) {
        Query query = entityManager.createQuery("select sp from StaticPage sp where sp.pageType = :pageType and sp.language = :language");
        query.setParameter("pageType", pageType);
        query.setParameter("language", language);
        try {
            StaticPage page = (StaticPage) query.getSingleResult();
            page.setContent(content);
        }
        catch (NoResultException e) {
            StaticPage page = new StaticPage(pageType, language);
            page.setContent(content);
            entityManager.persist(page);
        }
    }

    @Transactional
    public User removeCarouselItem(User user, Long savedItemId) {
        // remove carousel item and give back a user
        if (savedItemId == null || user == null) {
            throw new IllegalArgumentException("Input parameter(s) null ");
        }
        SavedItem savedItem = fetchSavedItem(user, savedItemId);
        if (savedItem == null) {
            throw new IllegalArgumentException("The user doesn't own the object. user: " + user.getId() + ", object: " + savedItemId);
        }
        CarouselItem carouselItem = savedItem.getCarouselItem();
        savedItem.setCarouselItem(null);
        entityManager.remove(carouselItem);
        entityManager.flush();
        user = entityManager.find(User.class, user.getId());
        return user;
    }

    @Transactional
    public Boolean removeCarouselItem(Long carouselItemId) {
        CarouselItem carouselItem = entityManager.getReference(CarouselItem.class, carouselItemId);
        if (carouselItem == null) {
            throw new IllegalArgumentException("Unable to find saved item: " + carouselItemId);
        }
        SavedItem savedItem = entityManager.getReference(SavedItem.class, carouselItem.getSavedItem().getId());
        if (savedItem == null) {
            throw new IllegalArgumentException("Unable to find saved item: " + carouselItemId);
        }
        savedItem.setCarouselItem(null);
        entityManager.remove(carouselItem);
        entityManager.flush();
        return true;
    }

    private SavedItem fetchSavedItem(User user, Long savedItemId) {
        // Query q = entityManager.createQuery("select o from SavedItem as o where userid = :userid and :id = id");

        // the previous instruction is incorrect. Maybe should be as follow, but is strange (id is filtered twice in the where clause)

        Query q = entityManager.createQuery("select o from SavedItem as o where o.id  = :userid and :id = id");
        q.setParameter("userid", user.getId());
        q.setParameter("id", savedItemId);
        List results = q.getResultList();
        if (results.size() != 1) {
            return null;
        }
        return (SavedItem) results.get(0);
    }

    @Transactional
    public User removeSearchTerm(User user, Long savedSearchId) {
        // remove carousel item and give back a user
        if (savedSearchId == null || user == null) {
            throw new IllegalArgumentException("Input parameter(s) null");
        }
        SavedSearch savedSearch = fetchSavedSearch(user, savedSearchId);
        if (savedSearch == null) {
            throw new IllegalArgumentException("The user doesn't own the object. user: " + user.getId() + ", object: " + savedSearchId);
        }
        SearchTerm searchTerm = savedSearch.getSearchTerm();
        savedSearch.setSearchTerm(null);
        entityManager.remove(searchTerm);
        entityManager.flush();
        user = entityManager.find(User.class, user.getId());
        return user;
    }

    private SavedSearch fetchSavedSearch(User user, Long savedSearchId) {
        //Query q = entityManager.createQuery("select o from SavedSearch as o where userid = :userid and :id = id");

        // the previous instruction is incorrect. Maybe should be as follow, but is strange (id is filtered twice in the where clause)


        Query q = entityManager.createQuery("select o from SavedSearch as o where o.id = :userid and :id = id");
        q.setParameter("userid", user.getId());
        q.setParameter("id", savedSearchId);
        List results = q.getResultList();
        if (results.size() != 1) {
            return null;
        }
        return (SavedSearch) results.get(0);
    }

    @Transactional
    public CarouselItem addCarouselItem(User user, Long savedItemId) {
//        SavedItem savedItem = fetchSavedItem(user, savedItemId);
        SavedItem savedItem = entityManager.getReference(SavedItem.class, savedItemId);
        if (savedItem == null) {
            throw new IllegalArgumentException("The user doesn't own the object. user: " + user.getId() + ", object: " + savedItemId);
        }
        CarouselItem carouselItem = savedItem.createCarouselItem();
        savedItem.setCarouselItem(carouselItem);
        return carouselItem;
    }

    @Transactional
    public User addCarouselItem(User user, SavedItem savedItem) {
        if (savedItem == null) {
            throw new IllegalArgumentException("The user doesn't own the object. user: " + user.getId() + ", object: " + savedItem.getId());
        }
        CarouselItem carouselItem = savedItem.createCarouselItem();
        savedItem.setCarouselItem(carouselItem);
        entityManager.persist(carouselItem);
        user = entityManager.merge(user);
        return user;
    }

    @Transactional
    public User addCarouselItem(User user, CarouselItem carouselItem) {
        //carouselItem.setetDateSaved(new Date());
        user = entityManager.merge(user);
        entityManager.persist(carouselItem);
        return user;
    }

    @Transactional
    public User addEditorPick(User user, EditorPick editorPick) {
        user = entityManager.merge(user);
        entityManager.persist(editorPick);
        return user;
    }

    @Transactional
    public SearchTerm addSearchTerm(Long savedSearchId) {
        SavedSearch savedSearch = entityManager.getReference(SavedSearch.class, savedSearchId);
        if (savedSearch == null) {
            throw new IllegalArgumentException("The user doesn't own the object. user:  object: " + savedSearchId);
        }
        SearchTerm searchTerm = savedSearch.createSearchTerm();
        savedSearch.setSearchTerm(searchTerm);
        return searchTerm;
    }


    @Transactional
    public boolean addSearchTerm(Language language, String term) {
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setLanguage(language);
        searchTerm.setProposedSearchTerm(term);
        searchTerm.setDate(new Date());
        entityManager.persist(searchTerm);
        return true; // maybe check for existence first?
    }

    @Transactional
    public boolean addSearchTerm(SavedSearch savedSearch) {
        SearchTerm searchTerm = savedSearch.createSearchTerm();
        entityManager.persist(searchTerm);
        return true;
    }

    @Transactional
    public boolean removeSearchTerm(Language language, String term) {
        // todo remove back reference to saved item
        Query query = entityManager.createQuery("delete from SearchTerm as term where term.language = :language and term.proposedSearchTerm = :term");
        query.setParameter("term", term);
        query.setParameter("language", language);
        boolean success = query.executeUpdate() == 1;
        if (!success) {
            log.warn("Not there to remove from search terms: " + term);
        }
        return success;
    }

    @Transactional
    public List<String> fetchSearchTerms(Language language) {
        Query query = entityManager.createQuery("select term.proposedSearchTerm from SearchTerm as term where term.language = :language");
        query.setParameter("language", language);
        return (List<String>) query.getResultList();
    }

    @Transactional
    public List<Partner> fetchPartners() {
        Query query = entityManager.createQuery("select p from Partner p order by p.sector");
        return (List<Partner>) query.getResultList();
    }

    @Transactional
    public List<Contributor> fetchContributors() {
        Query query = entityManager.createQuery("select c from Contributor c order by c.providerId");
        return (List<Contributor>) query.getResultList();
    }

    @Transactional
    public boolean removePartner(Long partnerId) {
        if (partnerId == null) {
            throw new IllegalArgumentException("The input parameter 'partnerId' is null");
        }
        Partner partner = entityManager.find(Partner.class, partnerId);
        if (partner != null) {
            entityManager.remove(partner);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean removeContributor(Long contributorId) {
        if (contributorId == null) {
            throw new IllegalArgumentException("The input parameter 'contributorId' is null");
        }
        Contributor contributor = entityManager.find(Contributor.class, contributorId);
        if (contributor != null) {
            entityManager.remove(contributor);
            return true;
        }
        return false;
    }

    @Transactional
    public StaticPage saveStaticPage(Long staticPageId, String content) {
        StaticPage page = entityManager.find(StaticPage.class, staticPageId);
        page.setContent(content);
        return page;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<CarouselItem> fetchCarouselItems() {
        Query q = entityManager.createQuery("select ci from CarouselItem ci");
        List<CarouselItem> results = (List<CarouselItem>) q.getResultList();
        for (CarouselItem item : results) {
            EuropeanaId id = item.getEuropeanaId();
            if (id != null && id.isOrphan()) { // remove null check later
                results.remove(item);
                removeCarouselItem(item.getSavedItem().getId());
            }
        }
        return results;
    }

    @Transactional
    public CarouselItem createCarouselItem(String europeanaUri, Long savedItemId) {
        EuropeanaId europeanaId = dashBoardDao.fetchEuropeanaId(europeanaUri);
        SavedItem savedItem = entityManager.getReference(SavedItem.class, savedItemId);
        CarouselItem carouselItem = savedItem.createCarouselItem();
        carouselItem.setEuropeanaId(europeanaId);
        carouselItem.setSavedItem(savedItem);
        savedItem.setCarouselItem(carouselItem);
        return carouselItem;
    }


    @Transactional
    public void removeFromCarousel(SavedItem savedItem) {
        CarouselItem carouselItem = savedItem.getCarouselItem();
        if (carouselItem != null) {
            savedItem = entityManager.getReference(SavedItem.class, savedItem.getId());
            savedItem.setCarouselItem(null);
            entityManager.persist(savedItem);
            carouselItem = entityManager.getReference(CarouselItem.class, carouselItem.getId());
            entityManager.remove(carouselItem);
        }
    }

    @Transactional
    public boolean addCarouselItem(SavedItem savedItem) {
        CarouselItem carouselItem = savedItem.createCarouselItem();
        //        carouselItem.setSavedItem(savedItem);
        savedItem.setCarouselItem(carouselItem);
        entityManager.persist(carouselItem);
        return true;
    }

    /*
    *  People Are Currently Thinking About, or editor picks
    */
    @Transactional
    public List<EditorPick> fetchEditorPicksItems() {
        Query query = entityManager.createQuery("select item from EditorPick item");
        return (List<EditorPick>) query.getResultList();
    }

    @Transactional
    public EditorPick createEditorPick(SavedSearch savedSearch) throws Exception {
        EditorPick editorPick = new EditorPick();
        editorPick.setDateSaved(savedSearch.getDateSaved());
        editorPick.setQuery(savedSearch.getQuery());
        editorPick.setUser(savedSearch.getUser());

        SavedSearch savedSearch2 = entityManager.getReference(SavedSearch.class, savedSearch.getId());
        editorPick.setSavedSearch(savedSearch2);
        savedSearch2.setEditorPick(editorPick);
        return editorPick;
    }

    @Transactional
    public void removeFromEditorPick(SavedSearch savedSearch) {
        EditorPick editorPick = savedSearch.getEditorPick();
        if (editorPick != null) {
            savedSearch = entityManager.getReference(SavedSearch.class, savedSearch.getId());
            savedSearch.setEditorPick(null);
            entityManager.persist(savedSearch);
            editorPick = entityManager.getReference(EditorPick.class, editorPick.getId());
            entityManager.remove(editorPick);
        }
    }

    @Transactional
    public List<SearchTerm> getAllSearchTerms() {
        Query q = entityManager.createQuery("select st from SearchTerm st");
        List searchTerms = q.getResultList();
        return (List<SearchTerm>) searchTerms;
    }
}