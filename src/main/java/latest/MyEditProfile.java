package latest;

import com.atlassian.core.user.GroupUtils;
import com.atlassian.core.user.UserUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.EntityNotFoundException;
import com.opensymphony.user.Group;
import com.opensymphony.user.User;
import com.opensymphony.util.TextUtils;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * @author dmitry dementiev
 */
//todo companyNAME!
public class MyEditProfile extends JiraWebActionSupport {
    private static final int MAX_LENGTH = 255;

    private String username;
    private String fullName;
    private String email;
    private String companyName;

    public String doDefault() throws Exception {
        final User current = getRemoteUser();

        if (current == null || !current.getName().equals(username)) {
            return ERROR;
        }

        fullName = current.getFullName();
        email = current.getEmail();
        //change company of user via propertyset
        List<String> groupsOfUser = current.getGroups();
        for (String groupOfUser : groupsOfUser) {
            if (TextUtils.stringSet(groupOfUser)) {
                PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(groupOfUser));
                if (propertySet.getBoolean("isCompany")) {
                    companyName = groupOfUser;
                }
            }
        }
        return super.doDefault();
    }

    public User getEditedUser() {
        return getRemoteUser();
    }


    protected void doValidation() {
        if (log.isDebugEnabled()) {
            log.debug("fullName = " + fullName);
            log.debug("email = " + email);
        }

        if (StringUtils.isBlank(fullName)) {
            addError("fullName", getText("admin.errors.invalid.full.name.specified"));
        } else if (fullName.length() > MAX_LENGTH) {
            addError("fullName", getText("signup.error.full.name.greater.than.max.chars"));
        }
        if (StringUtils.isBlank(email)) {
            addError("email", getText("admin.errors.invalid.email"));
        } else if (email.length() > MAX_LENGTH) {
            addError("email", getText("signup.error.email.greater.than.max.chars"));
        }
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception {
        final User current = getRemoteUser();

        if (current == null || !current.getName().equals(username)) {
            return ERROR;
        }

        current.setFullName(fullName);
        current.setEmail(email);
        //change company of user via propertyset
        List<String> groupsOfUser = getEditedUser().getGroups();
        for (String groupOfUser : groupsOfUser) {
            if (TextUtils.stringSet(groupOfUser)) {
                PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(groupOfUser));
                if (propertySet.getBoolean("isCompany")) {
                    current.removeFromGroup(GroupUtils.getGroup(groupOfUser));//remove old company
                }
            }
        }
        if (GroupUtils.existsGroup(getCompanyName())) {//set new company(group to user)
            current.addToGroup(GroupUtils.getGroup(getCompanyName()));
        }


        return returnComplete("ViewProfile.jspa");

    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return groups that have propertySet isCompany == 1
     */
    public Collection<Group> getCompanies() throws SQLException {
        Collection<Group> groups = GroupUtils.getGroups();
        //todo remove duplicates, stay only companies (isCompany==1)
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
