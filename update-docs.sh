#!/bin/sh

mvn clean
git worktree prune
if git worktree add target/generated-docs gh-pages
then
  mvn verify
  cd target/generated-docs
  git commit -a 
fi
