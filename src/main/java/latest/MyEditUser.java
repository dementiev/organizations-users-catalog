package latest;

import com.atlassian.core.user.GroupUtils;
import com.atlassian.core.user.UserUtils;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.user.GenericEditProfile;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.EntityNotFoundException;
import com.opensymphony.user.Group;
import com.opensymphony.user.User;
import com.opensymphony.util.TextUtils;
import latest.util.I18n;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;

/**
 * @author dmitry dementiev
 *         besides custom name&email also edit company name(group with propertyset isCompany==1)
 */
public class MyEditUser extends GenericEditProfile implements I18n {
    private final static String BUNDLE_NAME = "dementiev.TextResource";
    private final I18nHelper i18Helper;

    String editName;
    User user;
    String companyName;

    public MyEditUser(JiraAuthenticationContext authenticationContext) {
        i18Helper = authenticationContext.getI18nHelper(BUNDLE_NAME);
    }

    public void doValidation() {
        super.doValidation();

        if (!isRemoteUserPermittedToEditSelectedUser()) {
            addErrorMessage(getText("admin.errors.must.be.sysadmin.to.edit.sysadmin"));
        }
    }

    public boolean isRemoteUserPermittedToEditSelectedUser() {
        return getEditedUser() != null && (isSystemAdministrator() || !getGlobalPermissionManager().hasPermission(Permissions.SYSTEM_ADMIN, getEditedUser()));
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception {
        if (getApplicationProperties().getOption(APKeys.JIRA_OPTION_USER_EXTERNALMGT)) {
            addErrorMessage(getText("admin.errors.cannot.edit.user"));
            return getResult();
        }

        String superResult = super.doExecute();

        if (SUCCESS.equals(superResult)) {
            //change company of user via propertyset
            if (TextUtils.stringSet(request.getParameter("checkbox_company"))) {
                List<String> groupsOfUser = getEditedUser().getGroups();
                for (String groupOfUser : groupsOfUser) {
                    if (TextUtils.stringSet(groupOfUser)) {
                        PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(groupOfUser));
                        if (propertySet.getBoolean("isCompany")) {
                            getEditedUser().removeFromGroup(GroupUtils.getGroup(groupOfUser));//remove old company
                        }
                    }
                }
                if (TextUtils.stringSet(getCompanyName())) {
                    if (GroupUtils.existsGroup(getCompanyName())) {//set new company(group to user)
                        getEditedUser().addToGroup(GroupUtils.getGroup(getCompanyName()));
                    }
                }
            } else {//only remove current company(if it is)
                List<String> groupsOfUser = getEditedUser().getGroups();
                for (String groupOfUser : groupsOfUser) {
                    if (TextUtils.stringSet(groupOfUser)) {
                        PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(groupOfUser));
                        if (propertySet.getBoolean("isCompany")) {
                            getEditedUser().removeFromGroup(GroupUtils.getGroup(groupOfUser));//remove old company
                        }
                    }
                }
            }
            return getRedirect("ViewUser.jspa?name=" + URLEncoder.encode(editName, "UTF8"));
        }
        return superResult;
    }

    public String getEditName() {
        return editName;
    }

    public void setEditName(String editName) {
        this.editName = editName;
    }

    public User getEditedUser() {
        if (user == null) {
            try {
                user = UserUtils.getUser(editName);
            }
            catch (EntityNotFoundException e) {
                return null;
            }
        }

        return user;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /**
     * @return groups that have propertySet isCompany == 1
     */
    public Collection<Group> getCompanies() throws SQLException {
        Collection<Group> groups = GroupUtils.getGroups();
        //remove duplicates, stay only companies (isCompany==1)
        HashSet h = new HashSet(groups);
        groups.clear();
        groups.addAll(h);
        Iterator<Group> it = groups.iterator();
        while (it.hasNext()) {
            Group group = it.next();
            PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(group.getName()));
            if (!propertySet.getBoolean("isCompany")) {
                it.remove();
            }
        }
        return groups;
    }

    public String getCurrentCompany() throws SQLException {
        String result = "";
        List<String> groupsOfUser = getEditedUser().getGroups();
        for (String groupOfUser : groupsOfUser) {
            if (TextUtils.stringSet(groupOfUser)) {
                PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(groupOfUser));
                if (propertySet.getBoolean("isCompany")) {
                    result = groupOfUser;
                }
            }
        }
        return result;
    }

    /**
     * This method returns Html Encoded Text from recource
     *
     * @param key
     * @return
     */
    public String getHtmlEncodedText(String key) {
        String txt = getText(key);
        if (TextUtils.stringSet(txt))
            return "";
        else
            return TextUtils.htmlEncode(txt).replace("'", "&#39;").replace(System.getProperty("line.separator"), "&#10;");
    }

    /**
     * @param key
     * @param locale
     * @return text from resource
     */
    public String getText(String key, Locale locale) {
        return i18Helper.getText(key, locale);
    }
}
