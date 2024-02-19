#/bin/sh

git switch main &&
git pull &&
git switch -c "e399b63c-64d4-403f-9275-75c5e1e1359b" &&
echo 'randomabc' >> "modules/e399b63c-64d4-403f-9275-75c5e1e1359b.md" &&
echo 'randomdef' >> "modules/c4bbc915-bd2f-4b67-a251-ea3c2076724c.md" &&
git add . &&
git commit -m "test" &&
glab auth login -h git.archi-lab.io -t glpat-ojYxGqop2kxx1LX6SqDC &&
glab mr create -b preview -t "Updates" -f -y --remove-source-branch -l "auto approved" &&
git switch main &&
git branch -D "e399b63c-64d4-403f-9275-75c5e1e1359b"
