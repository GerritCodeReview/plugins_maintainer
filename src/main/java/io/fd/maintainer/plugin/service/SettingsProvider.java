/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.maintainer.plugin.service;

import static java.lang.String.format;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fd.maintainer.plugin.service.dto.PluginBranchSpecificSettings;
import io.fd.maintainer.plugin.util.ClosestMatch;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class SettingsProvider implements ClosestMatch {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsProvider.class);

    private static final String MAINTAINER_PLUGIN = "maintainer";
    private static final String BRANCH_SECTION = "branch";

    private static final String PLUGIN_USER = "pluginuser";

    private static final String MAINTAINERS_FILE_PATH_REF = "maintainerfileref";
    private static final String DEFAULT_MAINTAINERS_FILE_PATH_REF = "master/HEAD";

    private static final String MAINTAINERS_FILE_REF = "maintainerfile";
    private static final String DEFAULT_MAINTAINERS_FILE_REF = "MAINTAINERS";

    private static final String ALLOW_SUBMIT = "allowmaintainersubmit";
    private static final boolean DEFAULT_ALLOW_SUBMIT = false;

    private static final String AUTO_ADD_REVIEWERS = "autoaddreviewers";
    private static final boolean DEFAULT_AUTO_ADD_REVIEWERS = false;

    private static final String AUTO_SUBMIT = "autosubmit";
    private static final boolean DEFAULT_AUTO_SUBMIT = false;

    private static final String DISLIKE_WARNINGS = "dislikewarnings";
    private static final boolean DEFAULT_DISLIKE_WARNINGS = false;

    @Inject
    private PluginConfigFactory cfg;

    public PluginBranchSpecificSettings getBranchSpecificSettings(@Nonnull final String branchName,
                                                                  @Nonnull final Project.NameKey projectKey) {

        final String fullBranchName = branchName.startsWith(RefNames.REFS_HEADS)
                ? branchName
                : RefNames.REFS_HEADS.concat(branchName);

        LOG.info("Reading configuration for branch {}", fullBranchName);
        return getSettingsForBranch(fullBranchName, closesBranchMatch(fullBranchName, projectKey), projectKey);
    }

    private PluginBranchSpecificSettings getSettingsForBranch(final String branchName, final String closestBranch,
                                                              final Project.NameKey projectKey) {
        return new PluginBranchSpecificSettings.PluginSettingsBuilder()
                .setPluginUserName(pluginUserOrThrow(branchName, closestBranch, projectKey))
                .setLocalFilePath(fileNameRefOrDefault(branchName, closestBranch, projectKey))
                .setFileRef(filePathRefOrDefault(branchName, closestBranch, projectKey))
                .setAllowMaintainersSubmit(allowMaintainersSubmitOrDefault(branchName, closestBranch, projectKey))
                .setAutoAddReviewers(autoAddReviewersOrDefault(branchName, closestBranch, projectKey))
                .setAutoSubmit(autoSubmitOrDefault(branchName, closestBranch, projectKey))
                .setDislikeWarnings(dislikeWarningsOrDefault(branchName, closestBranch, projectKey))
                .setBranch(projectSpecificPluginConfig(projectKey).getSubsections(BRANCH_SECTION)
                        .stream()
                        .filter(subSection -> subSection.equals(branchName))
                        .findAny()
                        .orElse(closestBranch))
                .createPluginSettings();
    }

    private Boolean autoAddReviewersOrDefault(final String branch, final String closesBranch,
                                              final Project.NameKey projectKey) {
        return getKey(projectKey, branch, closesBranch, AUTO_ADD_REVIEWERS, DEFAULT_AUTO_ADD_REVIEWERS,
                Boolean::valueOf);
    }

    private Boolean autoSubmitOrDefault(final String branch, final String closestBranch,
                                        final Project.NameKey projectKey) {
        return getKey(projectKey, branch, closestBranch, AUTO_SUBMIT, DEFAULT_AUTO_SUBMIT, Boolean::valueOf);
    }

    private Boolean allowMaintainersSubmitOrDefault(final String branch, final String closesBranch,
                                                    final Project.NameKey projectKey) {
        return getKey(projectKey, branch, closesBranch, ALLOW_SUBMIT, DEFAULT_ALLOW_SUBMIT, Boolean::valueOf);
    }

    private Boolean dislikeWarningsOrDefault(final String branch, final String closesBranch,
                                             final Project.NameKey projectKey) {
        return getKey(projectKey, branch, closesBranch, DISLIKE_WARNINGS, DEFAULT_DISLIKE_WARNINGS, Boolean::valueOf);
    }

    private String fileNameRefOrDefault(final String branch, final String closesBranch,
                                        final Project.NameKey projectKey) {
        return getKey(projectKey, branch, closesBranch, MAINTAINERS_FILE_REF, DEFAULT_MAINTAINERS_FILE_REF,
                String::valueOf);
    }

    private String filePathRefOrDefault(final String branch, final String closesBranch,
                                        final Project.NameKey projectKey) {
        return getKey(projectKey, branch, closesBranch, MAINTAINERS_FILE_PATH_REF, DEFAULT_MAINTAINERS_FILE_PATH_REF,
                String::valueOf);
    }

    private String pluginUserOrThrow(final String branch,
                                     final String alternativeBranch,
                                     final Project.NameKey projectKey) {
        final Config config = projectSpecificPluginConfig(projectKey);
        return Optional.ofNullable(config.getString(BRANCH_SECTION, branch, PLUGIN_USER))
                .orElse(Optional.ofNullable(config.getString(BRANCH_SECTION, alternativeBranch, PLUGIN_USER))
                        .orElseThrow(() -> {
                            LOG.error("Plugin user not specified for branch {}", branch);
                            return new IllegalStateException(format("Plugin user not specified for branch %s", branch));
                        }));
    }

    private <T> T getKey(final Project.NameKey projectKey,
                         final String branch,
                         final String alternativeBranch,
                         final String subKey,
                         final T defaultValue,
                         final Function<String, T> mapTo) {
        return Optional.ofNullable(projectSpecificPluginConfig(projectKey)
                .getString(BRANCH_SECTION, branch, subKey))
                .map(mapTo)
                .orElse(Optional.ofNullable(
                        projectSpecificPluginConfig(projectKey).getString(BRANCH_SECTION, alternativeBranch, subKey))
                        .map(mapTo)
                        .orElse(defaultValue));
    }

    private Config projectSpecificPluginConfig(final Project.NameKey projectKey) {
        try {
            return cfg.getProjectPluginConfig(projectKey, MAINTAINER_PLUGIN);
        } catch (NoSuchProjectException e) {
            throw new IllegalStateException(format("Project %s not found", projectKey));
        }
    }

    // match by the number of changes needed to change one String into another
    private String closesBranchMatch(final String branchName, final Project.NameKey projectKey) {
        return projectSpecificPluginConfig(projectKey).getSubsections(BRANCH_SECTION).stream()
                .reduce((branchOne, branchTwo) -> closestMatch(branchName, branchOne, branchTwo))
                // if non use default
                .orElse(RefNames.REFS_HEADS);
    }
}
