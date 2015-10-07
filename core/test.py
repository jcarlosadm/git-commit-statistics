
map = {"a": [["0","0","0"],["0","0","0"],["0","0","0"]], "b" : [["0","0","0"],["0","1","0"],["0","0","0"]], "c": [["0","0","0"],["1","0","0"],["0","0","0"]]}
removeList = []
for key in map:
	remove = True
	print key
	for val in map[key]:
		if val[0] != "0" or val[1] != "0":
			remove = False
	if remove:
		removeList.append(key)
for key in removeList:
	del map[key]
print map
