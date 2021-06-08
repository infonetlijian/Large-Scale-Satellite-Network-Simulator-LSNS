# Large-Scale Satellite Network Simulator (LSNS)

LSNS is an open-source network simulator based on [ONE][1] simulator, it is designed to better support simulations under large-scale satellite networks and provide friendly interactive GUI.   
![LSNS](https://github.com/infonetlijian/ONE-Extended-Simulator/raw/gh-pages/images/icon.png)  
<br>
LSNS is developed and maintained by Infonet, USTC.
# How often will we update?

Since the program is sponsored, LSNS will not be updated very often on Github, some functions will only be avaiable in our group. If you have further questions, you are welcome to send your questions to the following e-mail: lijian9@ustc.edu.cn and hclu@ustc.edu.cn.

# Contributing

Bug reports and pull requests are welcome on GitHub at https://github.com/infonetlijian/Large-scale-Satellite-Network-Simulator.

# Development

We develop LSNS simulator based on [ONE][1] simulator, you can use the architecture built by LSNS and develop your own function by using IDE such as Eclipse or Intellij idea (preferred). You can find the main function in "ONE-Extended-Simulator/core/DTNSim.java". Currently, we offer two GUI options: 
<br>
1. default 2D GUI as the same in ONE simulator by change "userSetting.GUI = false" in "default_settings.txt"; 
<br>
2. 3D GUI which can display the orbit of satellites by setting "userSetting.GUI = true" in "default_settings.txt".
<br>
We are still working on 3D GUI, but it's not our primary task, if you have experience in developing 3D GUI, you are welcome to be our contributor and commit your code.

# How do I use LSNS?

You can download our code and import it as a new Java project in Eclipse or Intellij idea:
<br>
For Eclipse: Please refer to [website][2];
<br>
For Intellij idea: Please refer to [website][3] (choose "Create project from existing sources" and add library in "ONE-Extended-Simulator/lib" folder);
<br>
LSNS will read "ONE-Extended-Simulator/default_settings.txt" file as its simulation setting, you can change "default_settings.txt" to realize your own simulation. Original setting file in ONE simulator is stored as "ONE-Extended-Simulator/default_settings_ONE_backup.txt", you can compare these two files and find the difference of settings between ONE and LSNS.
<br>
We are trying to improve our code and make a stable version, but you can still build the program and have your own executable Jar file based on our current version.
<br>
If you have more questions about the mechanism of the simulator, you can also refer to [Q&A][4] of ONE simulator and other specific blogs like [Spark & Shine][5].

# Update Log
**v0.1**
<br>
1.Support 3D GUI; 
<br>
2.Support multi-layer satellite networks;
<br>
3.Add dynamic cluster algorithm (only avaliable in internal version); 
<br>
4.Add laser inter-satellite link module; 
<br>
5.Add link interruption module; 
<br>
6.Add in-network caching module; 
<br>

**v0.11**
<br>
1.Add wireless channel module;
<br>
2.Add ground-satellite relay routing module;
<br>
3.Fix bugs;
<br>

**v0.12**
<br>
1.Support neural network-based routing;
<br>
2.Add GNN-based routing module (Maven and Tensorflow required);
<br>
2.Format update;

[1]:https://akeranen.github.io/the-one/
[2]:https://stackoverflow.com/questions/20170470/importing-class-java-files-in-eclipse
[3]:https://www.jetbrains.com/help/idea/import-project-or-module-wizard.html
[4]:https://www.netlab.tkk.fi/tutkimus/dtn/theone/qa.html
[5]:http://sparkandshine.net/the-one-use-notes-directory/