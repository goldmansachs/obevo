/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.api.platform;

import com.gs.obevo.api.ChangesetNamePredicate;
import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecutionAttribute;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Sets;

public class MainDeployerArgs {
    public static final boolean DEFAULT_NOPROMPT_VALUE_FOR_API = true;
    private Predicate<? super Change> changeInclusionPredicate = Predicates.alwaysTrue();
    private Predicate<? super ChangeCommand> changesetPredicate;
    private boolean rollback = false;
    private boolean preview = false;
    private boolean performInitOnly = false;
    private boolean useBaseline = false;
    private boolean noPrompt = DEFAULT_NOPROMPT_VALUE_FOR_API;  // for API usage, we default noPrompt to true (as opposed to command-line, where we default to false)
    private boolean onboardingMode;
    private String productVersion;
    private String reason;
    private ImmutableSet<? extends DeployExecutionAttribute> deployExecutionAttributes = Sets.immutable.empty();
    private String deployRequesterId;

    public Predicate<? super Change> getChangeInclusionPredicate() {
        return changeInclusionPredicate;
    }

    public void setChangeInclusionPredicate(Predicate<? super Change> changeInclusionPredicate) {
        this.changeInclusionPredicate = changeInclusionPredicate;
    }

    public boolean isRollback() {
        return rollback;
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public boolean isPerformInitOnly() {
        return performInitOnly;
    }

    public void setPerformInitOnly(boolean performInitOnly) {
        this.performInitOnly = performInitOnly;
    }

    public boolean isUseBaseline() {
        return useBaseline;
    }

    public void setUseBaseline(boolean useBaseline) {
        this.useBaseline = useBaseline;
    }

    public boolean isNoPrompt() {
        return noPrompt;
    }

    public void setNoPrompt(boolean noPrompt) {
        this.noPrompt = noPrompt;
    }

    public MainDeployerArgs changeInclusionPredicate(final Predicate<? super Change> changeInclusionPredicate) {
        this.changeInclusionPredicate = changeInclusionPredicate;
        return this;
    }

    public MainDeployerArgs rollback(final boolean rollback) {
        this.rollback = rollback;
        return this;
    }

    public MainDeployerArgs preview(final boolean preview) {
        this.preview = preview;
        return this;
    }

    public MainDeployerArgs performInitOnly(final boolean performInitOnly) {
        this.performInitOnly = performInitOnly;
        return this;
    }

    public MainDeployerArgs useBaseline(final boolean useBaseline) {
        this.useBaseline = useBaseline;
        return this;
    }

    public MainDeployerArgs noPrompt(final boolean noPrompt) {
        this.noPrompt = noPrompt;
        return this;
    }

    public boolean isOnboardingMode() {
        return onboardingMode;
    }

    public void setOnboardingMode(boolean onboardingMode) {
        this.onboardingMode = onboardingMode;
    }

    public MainDeployerArgs onboardingMode(boolean onboardingMode) {
        this.setOnboardingMode(onboardingMode);
        return this;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public MainDeployerArgs productVersion(String productVersion) {
        this.setProductVersion(productVersion);
        return this;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public MainDeployerArgs reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public ImmutableSet<? extends DeployExecutionAttribute> getDeployExecutionAttributes() {
        return deployExecutionAttributes;
    }

    @SuppressWarnings("WeakerAccess")
    public void setDeployExecutionAttributes(ImmutableSet<? extends DeployExecutionAttribute> deployExecutionAttributes) {
        this.deployExecutionAttributes = deployExecutionAttributes != null ? deployExecutionAttributes : Sets.immutable.<DeployExecutionAttribute>empty();
    }

    public MainDeployerArgs deployExecutionAttributes(ImmutableSet<? extends DeployExecutionAttribute> deployExecutionAttributes) {
        this.setDeployExecutionAttributes(deployExecutionAttributes);
        return this;
    }

    public String getDeployRequesterId() {
        return deployRequesterId;
    }

    @SuppressWarnings("WeakerAccess")
    public void setDeployRequesterId(String deployRequesterId) {
        this.deployRequesterId = deployRequesterId;
    }

    public MainDeployerArgs deployRequesterId(String deployRequesterId) {
        this.setDeployRequesterId(deployRequesterId);
        return this;
    }

    public Predicate<? super ChangeCommand> getChangesetPredicate() {
        return changesetPredicate;
    }

    @SuppressWarnings("WeakerAccess")
    public void setChangesetPredicate(Predicate<? super ChangeCommand> changesetPredicate) {
        this.changesetPredicate = changesetPredicate;
    }

    public MainDeployerArgs changesetPredicate(Predicate<? super ChangeCommand> changesetPredicate) {
        this.setChangesetPredicate(changesetPredicate);
        return this;
    }

    public void setChangesetNames(final ImmutableSet<String> changesetNames) {
        this.setChangesetPredicate(new ChangesetNamePredicate(changesetNames));
    }

    public MainDeployerArgs changesetNames(final ImmutableSet<String> changesetNames) {
        this.setChangesetNames(changesetNames);
        return this;
    }

    public void setAllChangesets(boolean allChangesets) {
        if (allChangesets) {
            this.setChangesetPredicate(Predicates.alwaysTrue());
        } else {
            this.setChangesetPredicate(null);
        }
    }

    public MainDeployerArgs allChangesets(boolean allChangesets) {
        this.setAllChangesets(allChangesets);
        return this;
    }
}
