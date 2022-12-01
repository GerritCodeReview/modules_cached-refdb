// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.cachedrefdb;

import static com.google.inject.Scopes.SINGLETON;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.name.Names;

public class LibModule extends LifecycleModule {
  public static final String LOCAL_DISK_REPOSITORY_MANAGER = "local_disk_repository_manager";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  protected void configure() {
    bind(GitRepositoryManager.class)
        .annotatedWith(Names.named(LOCAL_DISK_REPOSITORY_MANAGER))
        .to(CachedGitRepositoryManager.class);

    DynamicItem.itemOf(binder(), RefByNameCache.class);
    DynamicItem.bind(binder(), RefByNameCache.class).to(NoOpRefByNameCache.class).in(SINGLETON);

    factory(RefUpdateWithCacheUpdate.Factory.class);
    factory(RefRenameWithCacheUpdate.Factory.class);
    factory(BatchRefUpdateWithCacheUpdate.Factory.class);
    factory(CachedRefDatabase.Factory.class);
    factory(CachedRefRepository.Factory.class);

    bind(GitRepositoryManager.class)
        .annotatedWith(Names.named(LOCAL_DISK_REPOSITORY_MANAGER))
        .to(CachedGitRepositoryManager.class);

    logger.atInfo().log("DB library loaded");
  }
}
