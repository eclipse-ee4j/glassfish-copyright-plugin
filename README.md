#   Copyright check/repair maven plugin

I've created a maven plugin to check for the proper copyright/license headers
and (in some cases) repair incorrect files.

Configure it as follows:

    <build>
        <plugins>
            <plugin>
                <groupId>org.glassfish.copyright</groupId>
                <artifactId>glassfish-copyright-maven-plugin</artifactId>
                <configuration>
                    <excludeFile>copyright-exclude</excludeFile>
                </configuration>
            </plugin>
        </plugins>
    </build>

To check copyrights and report errors:

    $ mvn glassfish-copyright:copyright

To only check copyrights, failing the build if there are any errors:

    $ mvn glassfish-copyright:check

To repair any errors discovered (use this carefully, and check the results):

    $ mvn glassfish-copyright:repair

You can add the following items in the configuration section:

    <excludeFile>file of exclude patterns</excludeFile>
    <exclude>
        <pattern>an exclude pattern</pattern>
    </exclude>
    <scm>svn|mercurial|git</scm>    <!-- defaults to svn -->
    <debug>true</debug>             <!--  turn on debugging -->
    <update>false</update>          <!--  for use with repair -->
    <warnings>false</warnings>      <!--  turn off warnings -->
    <ignoreYear>true</ignoreYear>   <!-- don't check that year is correct -->
    <scmOnly>true</scmOnly>         <!--  skip files not under SCM -->
    <templateFile>file containg template</templateFile>
    <alternateTemplateFile>alterate template file</alternateTemplateFile>
    <alternateTemplateFiles>
      <alternateTemplateFile>alterate template file1</alternateTemplateFile>
      <alternateTemplateFile>alterate template file2</alternateTemplateFile>
    </alternateTemplateFiles>
    <bsdTemplateFile>file containg BSD template</bsdTemplateFile>
    <useDash>true</useDash>        <!--  use dash instead of comma in years -->
    <normalize>true</normalize> <!-- normalize format of repaired copyright -->
    <preserveCopyrights>true</preserveCopyrights>
                                    <!-- preserve original copyright entries -->

Additionally, `check` goal accepts:

    <quiet>true</quiet> <!-- true/false: do not report/report failures -->


There are various errors that this plugin will correct:

- no copyright at all; these are the most likely cases for the plugin to
  do the wrong thing.
- a known, but incorrect, copyright.
- the correct copyright, but the copyright year wasn't updated.

Note that the repair option doesn't know what the copyright for a
file *should* be.  If the only thing wrong is the date, it just fixes
it.  But if the header is wrong it assumes the file should have the
EPL+GPL copyright, and replaces any existing copyright with that
(or whatever license you've set as the template).

If the file has an EDL/BSD license, it relaces it with the standard EDL
license.  If the file has a known Apache license, it tries to preserve
that.  However, if the file is *intended* to have one of the Apache
copyright/license headers or the BSD license (for example), but
doesn't, or it isn't in the expected format, the wrong license will
be applied.

If you have files that should be excluded from the checks (e.g.,
because they purposely have a different license), you can use the
<exclude> option to list them.  You can repeat the <pattern> as many times
as you need it, or you can put the names in the file and specify the
file name to the <excludeFile> option.
The excluded names are *substrings* (not regular expressions) that
are matched against the path/file name.

Good entries for an exclude list are:

    /MANIFEST.MF
    /META-INF/services/
    /README
    .gif
    .jpg
    .png
    .exe
    .ico
    .jar
    .zip
    .war
    .sql
    .jks
    .json
    .class


You can also run the copyright plugin without using maven (assuming you've
run it with maven at least once to load it into your local repository) using
a script such as this (I call it "cr"):

    #!/bin/sh
    repo=~/.m2/repository/org/glassfish/copyright/glassfish-copyright-maven-plugin
    v=`ls $repo | grep '^[1-9]' | tail -1`
    java -cp $repo/$v/glassfish-copyright-maven-plugin-$v.jar \
        org.glassfish.copyright.Copyright "$@"

This allows more fine grained control over which files are checked
(and repaired).

Use "cr -?" to get a list of options.
