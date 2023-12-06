# What is this?
This is a work-in-progress conversion of https://josm.openstreetmap.de/svn/ from svn to git.

GitHub is [ending support](https://github.blog/2023-01-20-sunsetting-subversion-support/)
for their SVN endpoints in January 2024. As such [JOSM](https://josm.openstreetmap.de/ticket/23286)
is looking at moving the plugin repository from SVN to git.

# Goals
* Idiomatic conversion from SVN to git
* Third-party patches should be properly attributed by the git author property
* Properly attribute commits (svn username -> name <public email>)

# Non-goals
* Repository size -- this can be done before the "final" conversion

# What can you do to help?
If you are a current or former contributor to JOSM or JOSM plugins (in SVN), please send
me ([Taylor Smock](mailto:tsmock@meta.com)) an email with the following information:
* The name you want us to use for git attribution
* The email you want us to use for git attribution -- this will be **public** via the git commit log!
  * [GitHub](https://docs.github.com/en/account-and-profile/setting-up-and-managing-your-personal-account-on-github/managing-email-preferences/setting-your-commit-email-address)
    has a `noreply` email address, usually formatted as `ID+USERNAME@users.noreply.github.com`
    for accounts created after 2017-07-18 or `USERNAME@users.noreply.github.com` for accounts
    created prior to 2017-07-18.
    Look at [Github Email Preferences](https://github.com/settings/emails) (under `Keep my email addresses private`).
  * [GitLab](https://docs.gitlab.com/ee/user/profile/index.html#use-an-automatically-generated-private-commit-email)
    has a `noreply` email address, usually formatted as `ID+USERNAME@users.noreply.gitlab.com`.
    Look at `Commit email` on your GitLab profile page.

# Tools used
* [reposurgeon](https://gitlab.com/esr/reposurgeon)
* [git lfs](https://github.com/git-lfs/git-lfs)
  * `git lfs track '*.jar' && git lfs migrate import --everything --include "*.jar"`
