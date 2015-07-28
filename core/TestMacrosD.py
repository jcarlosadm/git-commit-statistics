commit_ids = ['1','2','3','4','5']
commitFileMap = {};
file1= [1,2,3]
file2=[3,4,5]
file3=[4,5,6]
commitMap = {'a': file1, 'b':file2}
commitMap2 = {'a': file1, 'b':file2}
commitMap3 = {'b': file3, 'c':file2}
commitMap4 = {'a': file1, 'c':file3}
commitMap5 = {'c': file1, 'b':file2}
commitFileMap['1'] = commitMap
commitFileMap['2'] = commitMap2
commitFileMap['3'] = commitMap3
commitFileMap['4'] = commitMap4
commitFileMap['5'] = commitMap5
resultMap = {}
for commit_id in commit_ids :
	for file in commitFileMap[commit_id]:
		if file not in resultMap:
			resultMap[file] = {'x':[], 'y':[]}
		resultMap[file]['x'].append(commit_id)
		resultMap[file]['y'].append(commitFileMap[commit_id][file])
print resultMap
		
