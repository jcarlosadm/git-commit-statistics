import argparse
import os
import subprocess
import sys
import git
import os

def analyseCode(path):
	for root, subFolder, files in os.walk(path):
	    for item in files:
		if item.endswith(".c") :
		    try:		        
	 	        os.system("srcML/src2srcml " + root + "/" + item + " > ../results/" + item.replace(".c", ".xml"))
                        os.system("python dmacros.py --all 6 -d ../results/")
                        os.remove("../results/" + item.replace(".c", ".xml"))
		        with open(root + "/" + item) as f:
		            count = sum(1 for _ in f)
		        count = ', ' + str(count) + '\n'
		        tf = open('../results/resultFile',"a+")
	    	        tf.writelines(count)
		        tf.close()
		    except:
		        print 'Fail'


	
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
    for commit_id in commit_ids[::-1]:
        print commit_id
        try:	
            tf = open('../results/resultFile',"a+")
            tf.writelines('Commit: ' + commit_id + '\n')
            tf.close()    	
        limit = limit + 1
	g.reset('--hard', commit_id)
	analyseCode(args.path)
        except:
            print 'Fail'        
        

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('path', nargs="?", default=".")
    parser.add_argument('--branch', '-b', default="master")
    parser.add_argument('--skip', '-s', type=int, default=1, help='use every n-th commit')
    args = parser.parse_args()

    main(args)
    sys.exit(0)
