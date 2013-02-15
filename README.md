com.plugin.autoflox
===================



- OVERVIEW: An Eclipse plugin for AutoFlox

  AutoFlox is orginally developed by Frolin Ocariza, Karthik Pattabiraman and Ali Mesbah in 2011 as a Crawljax plugin. It is designed to localize JavaScript faults. 

  More info about AutoFlox can be found here:
  http://blogs.ubc.ca/karthik/files/2012/01/root_cause_paper.pdf

  I got an opportunity to work on the project with Dr. Pattabiraman in Jan, 2013. And I was advised to build an Eclipse plugin for AutoFlox. The goal is to make AutoFlox easy to use and possibly extend it.




- HOW TO

  1. Assuming you are developing an web app with some JavaScript in Eclipse.
  2. Make the workspace of Eclipse a web host directory. 
  3. Install or test the plugin from Eclipse IDE, open the AutoFlox view console in 'Wnndow -> Show View -> Other'.
  4. Open any file in your project that is under test in Eclipse editor, and click on 'Run AutoFlox' button on the tool bar.
  5. Navigate your browser to 'PathToWorkspace/autoflox_proxy/instrumented/', then you should be able to see the instrumented project that is under test. 
  6. Run this instrumented web app, you should be able to see a real time output about direct DOM access functions that cause errors in the web app.




- ENVIRONMENT
  
  Although the plugin is designed for cross-platform uses, I only tested it in Unbuntu 12.04 with open-jre-6, Firefox 15.0.1




- VIDEO DEMO

  The video demos what I can so far, I will keep this link updated. (Turn off html5 trail @ www.youtube.com/html5 if you encouter any decoding problem)
  http://www.youtube.com/watch?v=rVCyWoJdacw&list=UUu2rRBCmShecHKmB-INAu-w&index=1
