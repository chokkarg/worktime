/*
 * Copyright 2013 Dirk Vranckaert
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

package eu.vranckaert.worktime.activities.projects;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.inject.Inject;

import eu.vranckaert.worktime.R;
import eu.vranckaert.worktime.constants.Constants;
import eu.vranckaert.worktime.constants.TrackerConstants;
import eu.vranckaert.worktime.model.Project;
import eu.vranckaert.worktime.model.TimeRegistration;
import eu.vranckaert.worktime.service.ProjectService;
import eu.vranckaert.worktime.service.TaskService;
import eu.vranckaert.worktime.service.TimeRegistrationService;
import eu.vranckaert.worktime.service.ui.StatusBarNotificationService;
import eu.vranckaert.worktime.service.ui.WidgetService;
import eu.vranckaert.worktime.utils.context.AsyncHelper;
import eu.vranckaert.worktime.utils.context.ContextUtils;
import eu.vranckaert.worktime.utils.context.IntentUtil;
import eu.vranckaert.worktime.utils.context.Log;
import eu.vranckaert.worktime.utils.tracker.AnalyticsTracker;
import eu.vranckaert.worktime.utils.view.actionbar.RoboSherlockActivity;
import eu.vranckaert.worktime.utils.view.actionbar.SyncDelegateListener;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;

/**
 * User: DIRK VRANCKAERT
 * Date: 06/02/11
 * Time: 03:51
 */
public class AddEditProjectActivity extends RoboSherlockActivity implements SyncDelegateListener {
    private static final String LOG_TAG = AddEditProjectActivity.class.getSimpleName();

    @InjectView(R.id.projectname) private EditText projectNameInput;
    @InjectView(R.id.projectcomment) private EditText projectCommentInput;
    @InjectView(R.id.projectname_required) private TextView projectnameRequired;
    @InjectView(R.id.projectname_unique) private TextView projectnameUnique;

    @Inject
    private ProjectService projectService;

    @Inject
    private TaskService taskService;

    @Inject
    private TimeRegistrationService timeRegistrationService;

    @Inject
    private WidgetService widgetService;

    @Inject
    private StatusBarNotificationService statusBarNotificationService;

    @InjectExtra(value = Constants.Extras.PROJECT, optional = true)
    private Project editProject;

    private AnalyticsTracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_add_edit_project);
        setSupportProgressBarIndeterminateVisibility(false);

        setTitle(R.string.lbl_add_project_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tracker = AnalyticsTracker.getInstance(getApplicationContext());
        tracker.trackPageView(TrackerConstants.PageView.ADD_EDIT_PROJECT_ACTIVITY);

        if (inUpdateMode()) {
            setTitle(R.string.lbl_edit_project_title);
            projectNameInput.setText(editProject.getName());
            projectCommentInput.setText(editProject.getComment());
        }
    }

    /**
     * Save the project.
     */
    public void performSave() {
        hideValidationErrors();

        final String name = projectNameInput.getText().toString();
        final String comment = projectCommentInput.getText().toString();
        if (name.length() > 0) {
            if (checkForDuplicateProjectNames(name)) {
                Log.d(getApplicationContext(), LOG_TAG, "A project with this name already exists... Choose another name!");
                projectnameUnique.setVisibility(View.VISIBLE);
            } else {
                ContextUtils.hideKeyboard(AddEditProjectActivity.this, projectNameInput);
                Log.d(getApplicationContext(), LOG_TAG, "Ready to save new project");

                AsyncTask<String, Void, Project> task = new AsyncTask<String, Void, Project>(){
                    @Override
                    protected void onPreExecute() {
                        setSupportProgressBarIndeterminateVisibility(true);
                    }

                    @Override
                    protected Project doInBackground(String... parameters) {
                        Project project;
                        if (!inUpdateMode()) {
                            project = new Project();
                        } else {
                            project = editProject;
                        }
                        project.setName(parameters[0]);
                        project.setComment(parameters[1]);

                        if (!inUpdateMode()) {
                            project = projectService.save(project);
                            tracker.trackEvent(
                                    TrackerConstants.EventSources.ADD_EDIT_PROJECT_ACTIVITY,
                                    TrackerConstants.EventActions.ADD_PROJECT
                            );
                            Log.d(getApplicationContext(), LOG_TAG, "New project persisted");
                        } else {
                            project = projectService.update(project);
                            tracker.trackEvent(
                                    TrackerConstants.EventSources.ADD_EDIT_PROJECT_ACTIVITY,
                                    TrackerConstants.EventActions.EDIT_PROJECT
                            );
                            Log.d(getApplicationContext(), LOG_TAG, "Project with id " + project.getId() + " and name " + project.getName() + " is updated");
                        }

                        return project;
                    }

                    @Override
                    protected void onPostExecute(Project project) {
                        if (inUpdateMode()) {
                            TimeRegistration latestTimeRegistration = timeRegistrationService.getLatestTimeRegistration();
                            timeRegistrationService.fullyInitialize(latestTimeRegistration);
                            if (latestTimeRegistration != null && latestTimeRegistration.getTask().getProject().getId().equals(project.getId())) {
                                Log.d(getApplicationContext(), LOG_TAG, "About to update the widget and notifications");

                                taskService.refresh(latestTimeRegistration.getTask());
                                projectService.refresh(latestTimeRegistration.getTask().getProject());
                                widgetService.updateAllWidgets();
                                statusBarNotificationService.addOrUpdateNotification(latestTimeRegistration);
                            }
                        }

                        setSupportProgressBarIndeterminateVisibility(false);
                        Intent intentData = new Intent();
                        intentData.putExtra(Constants.Extras.PROJECT, project);
                        setResult(RESULT_OK, intentData);
                        finish();
                    }
                };
                AsyncHelper.startWithParams(task, new String[]{name, comment});
            }
        } else {
            Log.d(getApplicationContext(), LOG_TAG, "Validation error!");
            projectnameRequired.setVisibility(View.VISIBLE);
        }
    }

    private boolean checkForDuplicateProjectNames(String projectName) {
        if (!inUpdateMode()) {
            return projectService.isNameAlreadyUsed(projectName);
        } else  {
            return projectService.isNameAlreadyUsed(projectName, editProject);
        }
    }

    /**
     * Hide all the custom_validation errors.
     */
    private void hideValidationErrors() {
        projectnameRequired.setVisibility(View.GONE);
        projectnameUnique.setVisibility(View.GONE);
    }

    /**
     * Checks if the activity is in update mode. If not it's create mode!
     * @return {@link Boolean#TRUE} if the project is about to be updated, {@link Boolean#FALSE} if in creation mode.
     */
    private boolean inUpdateMode() {
        if (editProject == null || editProject.getId() < 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getSupportMenuInflater();
        menuInflater.inflate(R.menu.ab_activity_add_project, menu);

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                IntentUtil.goBack(AddEditProjectActivity.this);
                break;
            case R.id.menu_add_project_activity_save:
                performSave();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tracker.stopSession();
    }

    @Override
    public void onSyncCompleted(boolean success) {
        if (inUpdateMode() && success) {
            if (projectService.checkProjectExisting(editProject)) {
                if (projectService.checkReloadProject(editProject)) {
                    projectService.refresh(editProject);
                    projectNameInput.setText(editProject.getName());
                    projectCommentInput.setText(editProject.getComment());
                }
            } else {
                setResult(RESULT_OK);
                finish();
            }
        }
    }
}
