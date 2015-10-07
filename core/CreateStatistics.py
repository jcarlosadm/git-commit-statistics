import argparse
import os
import subprocess
import sys
import git
import os
import dmacros
import threading
import json
import time

def analyseCode(path, changedFiles):
	resultMap = {}
	testQueue = []
	for root, subFolder, files in os.walk(path):
	    for item in files:
		if item.endswith(".c") and item in changedFiles :
		    if item.replace('.c', '') not in testQueue:
		    	testQueue.append(item.replace('.c', ''))
			try:			
				t = threading.Thread(target=threadWork, args=(resultMap, testQueue, item, root))
				t.setDaemon(True)
				t.start()				
			except Exception,e: 
				print str(e)	
				try:
					del testQueue[testQueue.index(item.replace('.c', ''))]
				except:
					print "Fail to delete "
	count = 0 
	while(len(testQueue) > 0 and count < 20) :
	     count = count + 1
	     time.sleep( 1 )
	
	return resultMap

def threadWork(resultMap, testQueue, item, root):   
	try:     
		os.system("srcML/src2srcml " + root + "/" + item + " > ../results/" + item.replace(".c", ".xml"))
		resultMap[item.replace('.c', '')] = doWork(item.replace(".c", ".xml"), root)
	except Exception,e: 
		print str(e)	
		print "Fail to check file"
	try:
		del testQueue[testQueue.index(item.replace('.c', ''))]
	except:
		print "Fail to delete "
	return
	



def doWork(filename, root):
	a = dmacros.DisciplinedAnnotations('../results/' + filename)
        os.remove("../results/" + filename)
        with open(root + "/" + filename.replace(".xml", ".c")) as f:
            count = sum(1 for _ in f)
        a.results.append(str(count))
	return a.results

	
def compareCommits(idBefore, idAfter, repo):
	currentCommit = repo.commit(idAfter)
	beforeCommit = repo.commit(idBefore)
	changed_files = []
	for x in currentCommit.diff(beforeCommit):
	    if x.b_blob is not None and x.b_blob.name not in changed_files:
		if ".c" in x.b_blob.name:
		    changed_files.append(x.b_blob.name)
	return changed_files

def backup(commit_ids, commitFileMap, index):
    resultMap = {}
    idx = 0
    lastListIndex = 0
    for commit_id in commit_ids[::-1]:
	if idx > 0:
	     try:
		if commit_id in commitFileMap:
			for filename in commitFileMap[commit_id]:
				if filename not in resultMap:
					resultMap[filename] = {'x':[], 'y':[]}
				lastListIndex = len(resultMap[filename]['y'])-1
				if lastListIndex < 1 or resultMap[filename]['y'][lastListIndex] != commitFileMap[commit_id][filename]:
					resultMap[filename]['x'].append(commit_id)
					resultMap[filename]['y'].append(commitFileMap[commit_id][filename])
   	     except Exception,e: 
		print 'Backup failed: ' + commit_id + ' reason: \n'
		print 'index: ', lastListIndex
		print str(e)	
		
    	idx =  idx + 1
	if idx > index:
		break
	removeList = []
	for key in resultMap:
		remove = True
		for val in resultMap[key]['y']:
			if val[0] != "0" or val[1] != "0":
				remove = False
		if remove:
			removeList.append(key)
	for key in removeList:
		del resultMap[key]

    f = open('../results/resultJson', 'w')
    f.write('//Number of commits: ' + str(idx) + '\n var info =')
    f.write(json.dumps(resultMap))
    f.write('\n')
    f.close()

def main(args):
    try:
        repo = git.Repo(args.path)
	g = git.Git(args.path)
    except:
        sys.exit("no such repo")

    try:
        text = repo.git.rev_list(args.branch).splitlines()
    except:
        sys.exit("no such branch")

    commit_ids = text[::args.skip]

    print "loaded %s commits" % len(commit_ids)
    open('../results/resultFile',"a").close()
    idx = 0
    init = 0	
    index = 0
    end = len(commit_ids) + 1
    print args
    if int(args.i) != 0:
        init = int(args.i)
    if int(args.f) != 0:
        end = int(args.f)

    commitFileMap = {}
    print "Init: ", init, " End: ", end
    for commit_id in commit_ids[::-1]:
	try:
		if index < end and index > init:
			print 'Analisando: ' + commit_id
		
			if idx > 0:
			    g.reset('--hard', commit_id)
			    commitFileMap[commit_id] = analyseCode(args.path, compareCommits(commit_ids[idx-1], commit_id, repo))
			idx = idx + 1
			backup(commit_ids, commitFileMap, idx)
			time.sleep( 1 )	

		elif index < end:
			print "Skipping: " + commit_id
		else:
			break
		index = index + 1
	except Exception,e: 
		print 'Failed: ' + commit_id + ' reason: \n'		
		print str(e)
		
    resultMap = {}
    
        

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('path', nargs="?", default=".")
    parser.add_argument('--branch', '-b', default="master")
    parser.add_argument('--i', '-i', default="0")
    parser.add_argument('--f', '-f', default="0")
    parser.add_argument('--skip', '-s', type=int, default=1, help='use every n-th commit')
    args = parser.parse_args()

    main(args)
    sys.exit(0)
