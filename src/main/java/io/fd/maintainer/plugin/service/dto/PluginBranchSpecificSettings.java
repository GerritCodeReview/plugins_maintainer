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

package io.fd.maintainer.plugin.service.dto;

import com.google.gerrit.reviewdb.client.RefNames;

public class PluginBranchSpecificSettings {

    private final String pluginUserName;
    private final String branch;
    private final boolean wildcardBranch;
    private final String fileRef;
    private final String localFilePath;
    private final boolean allowMaintainersSubmit;
    private final boolean autoAddReviewers;
    private final boolean autoSubmit;
    private final boolean dislikeWarnings;

    private PluginBranchSpecificSettings(final String pluginUserName,
                                         final String branch,
                                         final boolean wildcardBranch,
                                         final String fileRef,
                                         final String localFilePath,
                                         final boolean allowMaintainersSubmit,
                                         final boolean autoAddReviewers,
                                         final boolean autoSubmit,
                                         final boolean dislikeWarnings) {
        this.pluginUserName = pluginUserName;
        this.branch = branch;
        this.wildcardBranch = wildcardBranch;
        this.fileRef = fileRef;
        this.localFilePath = localFilePath;
        this.allowMaintainersSubmit = allowMaintainersSubmit;
        this.autoAddReviewers = autoAddReviewers;
        this.autoSubmit = autoSubmit;
        this.dislikeWarnings = dislikeWarnings;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public boolean isAllowMaintainersSubmit() {
        return allowMaintainersSubmit;
    }

    public boolean isAutoAddReviewers() {
        return autoAddReviewers;
    }

    public boolean isAutoSubmit() {
        return autoSubmit;
    }

    /**
     * Rules for forming valid maintainer file ref are: <li>If branch section is wildcard - file ref will be created as
     * combination of branch name part without wildcard + file ref</li> <li>If branch sections is not wildcard - name of
     * the branch it self wil be used as ref </li> <li>If branch section does not contain refs/heads/ - use refs/heads/
     * + fileref</li>
     */
    public String fullFileRef() {
        // if for whatever reason someone uses some crazy wildcard like refs/*
        if (!branch.contains(RefNames.REFS_HEADS)) {
            return RefNames.REFS_HEADS + fileRef;
        }

        if (wildcardBranch) {
            return branch + fileRef;
        } else {
            return branch;
        }
    }

    public String getPluginUserName() {
        return pluginUserName;
    }

    public boolean isDislikeWarnings() {
        return dislikeWarnings;
    }


    public static class PluginSettingsBuilder {
        private String pluginUserName;
        private String branch;
        private boolean wildcardBranch;
        private String fileRef;
        private String localFilePath;
        private boolean allowMaintainersSubmit;
        private boolean autoAddReviewers;
        private boolean autoSubmit;
        private boolean dislikeWarnings;

        private String reduceWildcard(String input) {
            if (input.endsWith("*")) {
                wildcardBranch = true;
                return input.substring(0, input.indexOf("*"));
            } else {
                wildcardBranch = false;
                return input;
            }
        }

        public PluginSettingsBuilder setPluginUserName(final String pluginUserName) {
            this.pluginUserName = pluginUserName;
            return this;
        }

        public PluginSettingsBuilder setFileRef(final String fileRef) {
            this.fileRef = fileRef;
            return this;
        }

        public PluginSettingsBuilder setLocalFilePath(final String localFilePath) {
            this.localFilePath = localFilePath;
            return this;
        }

        public PluginSettingsBuilder setAllowMaintainersSubmit(final boolean allowMaintainersSubmit) {
            this.allowMaintainersSubmit = allowMaintainersSubmit;
            return this;
        }

        public PluginSettingsBuilder setAutoAddReviewers(final boolean autoAddReviewers) {
            this.autoAddReviewers = autoAddReviewers;
            return this;
        }

        public PluginSettingsBuilder setBranch(final String branch) {
            this.branch = reduceWildcard(branch);
            return this;
        }

        public PluginSettingsBuilder setAutoSubmit(final boolean autoSubmit) {
            this.autoSubmit = autoSubmit;
            return this;
        }

        public PluginSettingsBuilder setDislikeWarnings(final boolean dislikeWarnings) {
            this.dislikeWarnings = dislikeWarnings;
            return this;
        }

        public PluginBranchSpecificSettings createPluginSettings() {
            return new PluginBranchSpecificSettings(pluginUserName, branch, wildcardBranch, fileRef, localFilePath,
                    allowMaintainersSubmit, autoAddReviewers, autoSubmit, dislikeWarnings);
        }
    }
}
