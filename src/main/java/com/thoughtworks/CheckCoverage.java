package com.thoughtworks;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.sql.*;
import java.util.Arrays;

public class CheckCoverage extends Builder {
    @DataBoundConstructor
    public CheckCoverage() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String sql="select value, root_id from `project_measures` as pm \n" +
                "inner join `snapshots` as s \n" +
                "on pm.`snapshot_id`=s.`id` \n" +
                "inner join `projects` as p\n" +
                "on s.`project_id`=p.`id`\n" +
                "inner join `metrics` as m\n" +
                "on pm.`metric_id`=m.`id`\n" +
                "where m.`name`='coverage'\n" +
                "and p.`name`='MarsRovers'\n" +
                "and p.`root_id` is NULL\n" +
                "order by created_at \n" +
                "desc\n" +
                "limit 2;";
        String connectionUrl = "jdbc:mysql://localhost:3306/sonar?useUnicode=true&amp;characterEncoding=utf8";
        String userName = "root";
        String password = "";
        String driver = "com.mysql.jdbc.Driver";

        Connection connection = null;
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(
                    connectionUrl, userName, password);
            compareCoverage(sql, connection, listener);
            connection.close();
        } catch (Exception e) {
            throw new AbortException("Exception thrown when running mantra:\n"+ Arrays.toString(e.getStackTrace()));
        }
        return true;
    }

    private void compareCoverage(String sql, Connection connection, BuildListener listener) throws SQLException, AbortException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        float coverage = 100;
        while (resultSet.next()) {
            float delta = resultSet.getFloat("value") - coverage;
            if(delta > 0) {
                throw new AbortException("Your build coverage drop by -"+delta);
            }
            coverage = resultSet.getFloat("value");
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Fail Build When Coverage Drop";
        }

    }
}
