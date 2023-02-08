# Plugin to cache Git repository's access to refdb

When [Serialize AccountCache series](https://gerrit-review.googlesource.com/c/gerrit/+/260992)
was introduced it simplified the cache eviction by always reaching out to JGit
for data. Unfortunately it comes with
[price](https://bugs.chromium.org/p/gerrit/issues/detail?id=14945)
which is especially high when *All-Users* repository is accessed through the
NFS and `core.trustFolderStat = false` is configured in
`${GERRIT_SITE}/etc/jgit.config` (quite common setup for HA/Multi-Site ens).

This plugin was developed to introduce an in-memory cache per thread which
reduces the price for reaching to refs in JGit. It is a Gerrit native
alternative (that can be applied to Gerrit 3.2) to work that is currently
under progress for [caching Refs in JGit](https://git.eclipse.org/r/c/jgit/jgit/+/186205).

Here is the short comparison of _heavy-refs-related_ operations performance.
The test scenario was to get random change details (over the same REST API that
is used in Gerrit's details page) in 8 parallel threads over 5mins period of
time. The `core.trustFolderStat = false` was set in
`${GERRIT_SITE}/etc/jgit.config`.
It was called against:
* vanilla Gerrit 3.1.16 version (marked as `stable-3.1` in the results)
* vanilla Gerrit 3.2.12 version (marked as `stable-3.2` in the results)
* Gerrit 3.2.14 with libCache module loaded (marked as `stable-3.2-libCache` in
  the results).

Note that `TRS` is `Reqs/Sec` for each Thread.

```
| version                           | TRS Avg | TRS Std Dev | TRS Max | Total Reqs/sec | Transfer/sec(MB)|
| --------------------------------- | ------- | ----------- | ------- | -------------- | --------------- |
| stable-3.1                        | 57,33   | 8,26        | 80      | 456,95         | 4,34            |
| stable-3.2                        | 13,87   | 4,92        | 20      | 110,18         | 1,07            |
| stable-3.2-libCache               | 105,27  | 14,55       | 150     | 834,88         | 8,41            |
| stable-3.1 vs stable-3.2          | 313,34% | 67,89%      | 300,00% | 314,73%        | 305,61%         |
| stable-3.2-libCache vs stable-3.2 | 658,98% | 195,73%     | 650,00% | 657,74%        | 685,98%         |
| stable-3.2-libCache vs stable-3.1 | 83,62%  | 76,15%      | 87,50%  | 82,71%         | 93,78%          |
```

One can clearly see that in this setup using this library module outperforms both
Gerrit 3.2 and 3.1 by factor of *6* and *2* correspondingly.
The test script, detailed description and more results are available
[here](https://gist.github.com/geminicaprograms/b2cae199793f0f2b18759a803000447f).

## How to build

Clone or link this plugin to the plugins directory of Gerrit's source tree,
and then run bazel build on the plugin's directory.

Example:

```
git clone --recursive https://gerrit.googlesource.com/gerrit
cd plugins
git clone "https://gerrit.googlesource.com/modules/cached-refdb"
cd .. && bazel build plugins/cached-refdb
```

The output plugin jar is created in:

```
bazel-bin/plugins/cached-refdb/cached-refdb.jar
```

## How to install

Copy the cached-refdb.jar into the `${GERRIT_SITE}/lib/` so that it is
being loaded when the Gerrit instance is started. Note that the following
configuration options need to be added

```
git config --file ${GERRIT_SITE}/etc/gerrit.config --add gerrit.installDbModule\
  com.googlesource.gerrit.plugins.cachedrefdb.LibDbModule
git config --file ${GERRIT_SITE}/etc/gerrit.config --add gerrit.installModule\
  com.googlesource.gerrit.plugins.cachedrefdb.LibSysModule
```

> NOTE: There are situations where the binding of the module to the Gerrit's
> GitRepositoryManager is not desired; e.g., when using this module together
> with others that are trying to override it at the same time.
>
> It is possible to just load the module using the following two options:
>
> ```
> git config --file ${GERRIT_SITE}/etc/gerrit.config --add gerrit.installDbModule\
>   com.googlesource.gerrit.plugins.cachedrefdb.LibModule
> git config --file ${GERRIT_SITE}/etc/gerrit.config --add gerrit.installModule\
>   com.googlesource.gerrit.plugins.cachedrefdb.LibSysModule
> ```

By default cache can hold up to `1024` refs which will not be sufficient for
any production site therefore one can configure it through the standard Gerrit
cache configuration means e.g.

```
git config --file ${GERRIT_SITE}/etc/gerrit.config cache.ref_by_name.memoryLimit 10240
```

Note that library module requires the Gerrit instance restart in order to pick
up the configuration changes.