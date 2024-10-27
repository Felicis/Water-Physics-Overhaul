#!/bin/bash

total_lines=0;
total_lines_unchanged=0;
total_lines_new=0;

echo "--- total, unchanged, new, filename ---"

for file in `git ls-files | grep -v "src/main/resources" | grep "src"`
do
  # calculate stats for this file
  lines=`cat $file | wc -l`
  lines_unchanged=`git blame c5939.. $file | grep -P "^\^c5939" | wc -l`
  lines_new=`git blame c5939.. $file | grep -Pv "^\^c5939" | wc -l`

  # print pretty
  lines_pretty="$(printf "%6d" $lines)"
  lines_unchanged_pretty="$(printf "%6d" $lines_unchanged)"
  lines_new_pretty="$(printf "%6d" $lines_new)"
  echo "$lines_pretty $lines_unchanged_pretty $lines_new_pretty $file"

  # add to total
  total_lines=$(($total_lines + $lines))
  total_lines_unchanged=$(($total_lines_unchanged + $lines_unchanged))
  total_lines_new=$(($total_lines_new + $lines_new))
done

# print total
echo "--- total, unchanged, new ---"

lines_pretty="$(printf "%6d" $total_lines)"
lines_unchanged_pretty="$(printf "%6d" $total_lines_unchanged)"
lines_new_pretty="$(printf "%6d" $total_lines_new)"
echo "$lines_pretty $lines_unchanged_pretty $lines_new_pretty TOTAL"

