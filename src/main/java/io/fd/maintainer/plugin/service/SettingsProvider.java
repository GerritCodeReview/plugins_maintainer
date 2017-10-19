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

import com.google.common.base.Strings;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fd.maintainer.plugin.service.dto.PluginBranchSpecificSettings;
import io.fd.maintainer.plugin.util.ClosestMatch;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

@Singleton
public class SettingsProvider implements ClosestMatch {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsProvider.class);

    private static final String MAINTAINER_PLUGIN = "maintainer";
    private static final String BRANCH_SECTION = "branch";

    private static final String PLUGIN_USER = "pluginuser";
    private static final String DEFAULT_PLUGIN_USER = "non-existing-user";

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


    private final PluginConfigFactory cfg;

    @Inject
    public SettingsProvider(PluginConfigFactory cfg) {
        this.cfg = cfg;
    }

    public PluginBranchSpecificSettings getBranchSpecificSettings(@Nonnull final String branchName,
                                                                  @Nonnull final Project.NameKey projectKey) {

        final String fullBranchName = branchName.startsWith(RefNames.REFS_HEADS)
                ? branchName
                : RefNames.REFS_HEADS.concat(branchName);

        LOG.info("Reading configuration for branch {}", fullBranchName);
        final Optional<String> closestBranch = closesBranchMatch(fullBranchName, projectKey);

        if (closestBranch.isPresent()) {
            // either current branch or some similar has config
            return getSettingsForBranch(fullBranchName, closestBranch.get(), projectKey);
        }

        //not current nor similar branch has config, therefore return default
        return new PluginBranchSpecificSettings.PluginSettingsBuilder()
                .setAllowMaintainersSubmit(DEFAULT_ALLOW_SUBMIT)
                .setAutoAddReviewers(DEFAULT_AUTO_ADD_REVIEWERS)
                .setAutoSubmit(DEFAULT_AUTO_SUBMIT)
                .setDislikeWarnings(DEFAULT_DISLIKE_WARNINGS)
                .setBranch(fullBranchName)
                .setFileRef(DEFAULT_MAINTAINERS_FILE_REF)
                .setLocalFilePath(DEFAULT_MAINTAINERS_FILE_PATH_REF)
                .setPluginUserName(DEFAULT_PLUGIN_USER)
                .createPluginSettings();
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

        final String user = Optional.ofNullable(config.getString(BRANCH_SECTION, branch, PLUGIN_USER))
                .orElse(config.getString(BRANCH_SECTION, alternativeBranch, PLUGIN_USER));

        if (Strings.isNullOrEmpty(user)) {
            LOG.error("Plugin user not specified for branch {}", branch);
            throw new IllegalStateException(format("Plugin user not specified for branch %s", branch));
        } else {
            return user;
        }
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
    private Optional<String> closesBranchMatch(final String branchName, final Project.NameKey projectKey) {
        final BranchInfo currentBranchInfo = new BranchInfo(branchName);
        return projectSpecificPluginConfig(projectKey).getSubsections(BRANCH_SECTION).stream()
                .map(BranchInfo::new)
                .filter(branchInfo -> branchInfo.isAlternativeFor(currentBranchInfo))
                .map(BranchInfo::getBranchPart)
                .reduce((branchOne, branchTwo) -> closestMatch(branchName, branchOne, branchTwo));
    }

    static class BranchInfo {

        private final boolean isWildcarded;
        private final boolean isGerritReviewBranch;
        private final String fullBranchName;
        private final String branchPart;

        BranchInfo(@Nonnull final String input) {
            checkNotNull(input, "Input for %s cannot be null", this.getClass().getName());
            final String[] parts = input.split("\\/");
            isWildcarded = parts[parts.length - 1].trim().equals("*");
            isGerritReviewBranch = input.trim().startsWith(RefNames.REFS_HEADS);
            fullBranchName = input.trim();
            if (isGerritReviewBranch) {
                branchPart = fullBranchName.replace(RefNames.REFS_HEADS, "").replace("/*", "");
            } else {
                branchPart = fullBranchName;
            }
        }

        public boolean isAlternativeFor(final BranchInfo other) {
            if (this.isGerritReviewBranch && other.isGerritReviewBranch) {
                // both branches are standard review branches like /refs/heads/master for ex.
                final String[] thisBranchParts = this.branchPart.split("\\/");
                final String[] otherBranchParts = other.branchPart.split("\\/");

                return thisBranchParts[0].equals(otherBranchParts[0]);
            }
            return fullBranchName.equals(other.fullBranchName);
        }

        public boolean isWildcarded() {
            return isWildcarded;
        }

        public boolean isGerritReviewBranch() {
            return isGerritReviewBranch;
        }

        public String getFullBranchName() {
            return fullBranchName;
        }

        public String getBranchPart() {
            return branchPart;
        }
    }
}
