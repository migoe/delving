package eu.europeana.controller;

import eu.europeana.controller.util.ControllerUtil;
import eu.europeana.database.StaticInfoDao;
import eu.europeana.database.UserDao;
import eu.europeana.database.domain.CarouselItem;
import eu.europeana.database.domain.SavedItem;
import eu.europeana.database.domain.SavedSearch;
import eu.europeana.database.domain.SearchTerm;
import eu.europeana.database.domain.SocialTag;
import eu.europeana.database.domain.User;
import javax.servlet.http.HttpServletRequest;

/**
 * Remove something associated with a user
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class RemoveController extends AbstractAjaxController {

    private UserDao userDao;
    private StaticInfoDao staticInfoDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void setStaticInfoDao(StaticInfoDao staticInfoDao) {
        this.staticInfoDao = staticInfoDao;
    }

    public boolean handleAjax(HttpServletRequest request) throws Exception {
        User user = ControllerUtil.getUser();
        String className = request.getParameter("className");
        String idString = request.getParameter("id");
        if (className == null || idString == null) {
            throw new IllegalArgumentException("Expected 'className' and 'id' parameters!");
        }
        Long id = Long.valueOf(idString);

        Class clazz = Class.forName("eu.europeana.database.domain." + className); // todo: remove later
        switch (findRemovable(className)) {
            case CAROUSEL_ITEM:
                user = staticInfoDao.removeCarouselItem(user, id);
                break;
            case SAVED_ITEM:
                user = userDao.remove(user, clazz, id);
                break;
            case SAVED_SEARCH:
                user = userDao.remove(user, clazz, id);
                break;
            case SEARCH_TERM:
                user = staticInfoDao.removeSearchTerm(user, id);
                break;
            case SOCIAL_TAG:
                user = userDao.remove(user, clazz, id);
                break;
            default:
                throw new IllegalArgumentException("Unhandled removable");
        }

        ControllerUtil.setUser(user);
        return true;
    }

    private Removable findRemovable(String className) {
        for (Removable removable : Removable.values()) {
            if (removable.matches(className)) {
                return removable;
            }
        }
        throw new IllegalArgumentException("Unable to find removable class with name "+className);
    }

    private enum Removable {
        SEARCH_TERM(SearchTerm.class),
        CAROUSEL_ITEM(CarouselItem.class),
        SAVED_ITEM(SavedItem.class),
        SAVED_SEARCH(SavedSearch.class),
        SOCIAL_TAG(SocialTag.class);

        private String className;

        private Removable(Class<?> clazz) {
            this.className = clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
        }

        public boolean matches(String className) {
            return this.className.equals(className);
        }
    }
}