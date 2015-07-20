# git-commit-statistics
Requeriments:
Instal git
instal python setup tools https://pypi.python.org/pypi/setuptools
Instal gitpython https://github.com/gitpython-developers/GitPython
Change src2srcml and srcml2src for you so correspondent, they're at core/srcML, download here (http://www.srcml.org/downloads.html -> http://www.sdml.info/lmcrs/)
Use example
Clone a github project
git clone https://github.com/GNOME/dia.git
On directory core, execute CreateStatistics.py
python CreateStatistics.py --branch master 'pathtoproject'
