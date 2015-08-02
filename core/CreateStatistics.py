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
	while(len(testQueue) > 0 and count < 10) :
	     count = count + 1
	     time.sleep( 2 )
	
	return resultMap

def threadWork(resultMap, testQueue, item, root):   
	try:     
		os.system("srcML/src2srcml " + root + "/" + item + " > ../results/" + item.replace(".c", ".xml"))
		resultMap[item.replace('.c', '')] = doWork(item.replace(".c", ".xml"), root)
	except:
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
    for commit_id in commit_ids:
	if idx > 0:
	     try:
		for filename in commitFileMap[commit_id]:
			if filename not in resultMap:
				resultMap[filename] = {'x':[], 'y':[]}
			resultMap[filename]['x'].append(commit_id)
			resultMap[filename]['y'].append(commitFileMap[commit_id][filename])
   	     except Exception,e: 
		print 'Failed: ' + commit_id + ' reason: \n'		
		print str(e)	
		
    	idx =  idx + 1
	if idx > index:
		break
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
	print args.branch
        text = repo.git.rev_list(args.branch).splitlines()
    except:
        sys.exit("no such branch")

    commit_ids = text[::args.skip]

    print "loaded %s commits" % len(commit_ids)
    open('../results/resultFile',"a").close()
    idx = 0
    commitFileMap = {}
    for commit_id in commit_ids[::-1]:
	try:
		print 'Analisando: ' + commit_id
		
		if idx > 0:
		    g.reset('--hard', commit_id)
		    commitFileMap[commit_id] = analyseCode(args.path, compareCommits(commit_ids[idx-1], commit_id, repo))
		idx = idx + 1
		backup(commit_ids[::-1], commitFileMap, idx)
		time.sleep( 1 )
	except Exception,e: 
		print 'Failed: ' + commit_id + ' reason: \n'		
		print str(e)
    resultMap = {}
    
        

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('path', nargs="?", default=".")
    parser.add_argument('--branch', '-b', default="master")
    parser.add_argument('--skip', '-s', type=int, default=1, help='use every n-th commit')
    args = parser.parse_args()

    main(args)
    sys.exit(0)
