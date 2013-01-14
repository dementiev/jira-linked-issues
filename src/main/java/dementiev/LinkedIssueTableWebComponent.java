package dementiev;

import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.layout.column.ColumnLayoutItem;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.preferences.PreferenceKeys;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.jira.util.collect.CompositeMap;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.jira.web.component.IssuePager;
import com.atlassian.jira.web.component.IssueTableLayoutBean;
import com.atlassian.jira.web.component.IssueTableWebComponent;
import com.atlassian.jira.web.component.IssueTableWriter;
import com.atlassian.jira.issue.link.*;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * @author dmitry demeniev
 */
public class LinkedIssueTableWebComponent extends IssueTableWebComponent {
    private final JiraAuthenticationContext authenticationContext = ComponentManager.getComponentInstanceOfType(JiraAuthenticationContext.class);

    private static final String ISSUETABLE_HEADER = "templates/jira/issue/table/issuetable-header.vm";
    private static final String ISSUETABLE_FOOTER = "templates/jira/issue/table/issuetable-footer.vm";
    private static final String ISSUETABLE_SINGLE_ISSUE = "templates/issue/table/issuetable-linkedissue.vm";
    private static final String CARROT_HIDDEN = "class=\"hide-carrot\"";

    /**
     * Constructs a new {@link com.atlassian.jira.web.component.IssueTableWriter} that can be used to write
     * a table of issues.
     *
     * @param writer          The writer to write the table to
     * @param layout          The layout describing how this table should look
     * @param pager           An optional pager which will be used for displaying the next / previous paging at the top and bottom
     * @param actionColumn    The column to display on the right hand side of the table.  If null, no column is shown.
     * @param selectedIssueId the issue to mark as selected in the table; if null, marks the first issue as selected
     * @return An IssueTableWriter which the caller should use to write each issue to.
     */
    public IssueTableWriter getHtmlIssueWriter(final Writer writer, final IssueTableLayoutBean layout, final IssuePager pager, final ColumnLayoutItem actionColumn, final Long selectedIssueId) {
        String carrothiddenString = "";
        boolean keyboadShortcutsEnabled = true;
        if (authenticationContext.getLoggedInUser() != null) {
            Preferences userPrefs = ComponentAccessor.getUserPreferencesManager().getPreferences(authenticationContext.getLoggedInUser());
            keyboadShortcutsEnabled = !userPrefs.getBoolean(PreferenceKeys.USER_KEYBOARD_SHORTCUTS_DISABLED);
        }
        if (!keyboadShortcutsEnabled)
            carrothiddenString = CARROT_HIDDEN;

        final Map<String, Object> params = getDefaultParams(MapBuilder.<String, Object>newBuilder()
                .add("layout", layout)
                .add("i18n", authenticationContext.getI18nHelper())
                .add("pager", pager)
                .add("columnTotals", null)
                .add("actionColumn", actionColumn)
                .add("selectedIssueId", selectedIssueId)
                .add("carrothidden", carrothiddenString)
                .toMap());

        return new IssueTableWriter() {
            int issueCount = 0;

            public void write(final Issue issue) throws IOException {
                issueCount++;
                if (issueCount == 1) {
                    writer.write(getHtml(ISSUETABLE_HEADER, params));
                }
                final Map<String, Object> issueParams = new HashMap<String, Object>();
                issueParams.put("issue", issue);
                IssueLinkManager issueLinkManager = ComponentManager.getInstance().getIssueLinkManager();
                IssueManager issueManager = ComponentManager.getInstance().getIssueManager();
                List<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(issue.getId());
                Set<Long> inwardLinkIds = new HashSet<Long>();
                for (IssueLink issueLink : inwardLinks) {
                    inwardLinkIds.add(issueLink.getSourceId());
                }
                if (inwardLinkIds.size() > 0) {
                    issueParams.put("linkedIssues", issueManager.getIssueObjects(inwardLinkIds));
                }
                issueParams.put("issueCount", issueCount);
                writer.write(getHtml(ISSUETABLE_SINGLE_ISSUE, CompositeMap.of(issueParams, params)));
            }

            public void close() throws IOException {
                if (issueCount > 0) {
                    writer.write(getHtml(ISSUETABLE_FOOTER, params));
                }
            }
        };
    }

    private Map<String, Object> getDefaultParams(final Map<String, Object> startingParams) {
        return JiraVelocityUtils.getDefaultVelocityParams(startingParams, authenticationContext);
    }
}
