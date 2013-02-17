An Eclipse plugin for AutoFlox
===================



OVERVIEW
----------------------------------------

  AutoFlox is orginally developed by Frolin Ocariza, Karthik Pattabiraman and Ali Mesbah in 2011 as a Crawljax plugin. It is designed to localize JavaScript faults. 

  More info about AutoFlox can be found [here](http://blogs.ubc.ca/karthik/files/2012/01/root_cause_paper.pdf).

  I got an opportunity to work on the project with Dr. Pattabiraman in Jan, 2013. And I was advised to build an Eclipse plugin for AutoFlox. The goal is to make AutoFlox easy to use and possibly extend it.




HOW TO
------

  * Assuming you are developing an web app with some JavaScript in Eclipse.
  * Make the workspace of Eclipse a web host directory. 
  * Install or test the plugin from Eclipse IDE, open the AutoFlox view console in 'Wnndow -> Show View -> Other'.
  * Open any file in your project that is under test in Eclipse editor, and click on 'Run AutoFlox' button on the tool bar.
  * Navigate your browser to 'PathToWorkspace/autoflox_proxy/instrumented/', then you should be able to see the instrumented project that is under test. 
  * Run this instrumented web app, you should be able to see a real time output about direct DOM access functions that cause errors in the web app.




ENVIRONMENT
-----------
  
  Although the plugin is designed for cross-platform uses, I only tested it in Unbuntu 12.04 with open-jre-6, Firefox 15.0.1




VIDEO DEMO
----------

  The [video](http://www.youtube.com/watch?v=rVCyWoJdacw&list=UUu2rRBCmShecHKmB-INAu-w&index=1) demos what I can so far, I will keep this link updated.
