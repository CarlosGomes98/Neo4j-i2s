package graph;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
/**
 * Deals with connection to the SQL server
 * @author i2scmg
 *
 */
public class SQLDB {
    private Connection connection;
    
    public SQLDB(Connection connection){
        this.connection = connection;
    }
    
    /**
     * Gets the question names from the questionaires.
     * @return Result with question names.
     */
    public ResultSet getQuestions(){
        String query = "SELECT DISTINCT"
                + "[QUESTION_KEY] "
                + "FROM [WF].[i2sflowbam].[QNRS_QNS] "
                + "ORDER BY [QUESTION_KEY]";
        Statement statement = null;
        try{
            statement = connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, 
            ResultSet.CONCUR_READ_ONLY);
        
        ResultSet resultSet = statement.executeQuery(query);
        return resultSet;
        }
        catch(Exception e){
            return null;
        }
    }
    
    /**
     * Gets the answers to the questionnaires.
     * @return Result with answers.
     */
    public ResultSet getAnswers(){
        String query = "SELECT"
                + "[ANSWER],"
                + "[PROCESS_ACTIVITY_KEY] "
                + "FROM [WF].[i2sflowbam].[QNR_ANWER]"
                + "ORDER BY [PROCESS_ACTIVITY_KEY], [QUESTION_KEY] ";
        Statement statement = null;
        try{
            statement = connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, 
            ResultSet.CONCUR_READ_ONLY);
        
        ResultSet resultSet = statement.executeQuery(query);
        return resultSet;
        }
        catch(Exception e){
            return null;
        }
    }
    
    /**
     * Gets all the activities from the database.
     * @return Result with activity names.
     */
    public ResultSet getActivities(){
        String query = "SELECT DISTINCT"
                + "[CURRENT_ACT_ID] "
                + "FROM [WF].[i2sflowbam].[PROC_GRAPH] ";
        Statement statement = null;
        try{
            statement = connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, 
            ResultSet.CONCUR_READ_ONLY);
        
        ResultSet resultSet = statement.executeQuery(query);
        return resultSet;
        }
        catch(Exception e){
            return null;
        }
    }
    
    /**
     * Gets all the processes from the database.
     * @return Result with processes.
     */
    public ResultSet getProcesses(){
        String query = "SELECT "
                + "[ID_NODE], "
                + "[PROC_GRAPH].[PROCESS_KEY], "
                + "[CURRENT_ACT_ID], "
                + "[NEXT_ACT_ID], "
                + "[REL_TIMESTAMP], "
                + "[DURATION] "
                + "FROM [WF].[i2sflowbam].[PROC_GRAPH] INNER JOIN [WF].[i2sflowbam].[PROC_EXECS] "
                + "ON [PROC_GRAPH].[PROCESS_KEY] = [PROC_EXECS].[PROCESS_KEY] AND CURRENT_ACT_ID = ACTIVITY_KEY "
                + "ORDER BY [PROC_GRAPH].[PROCESS_KEY]";
        
        try{
        Statement statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, 
                ResultSet.CONCUR_READ_ONLY);
        ResultSet resultSet = statement.executeQuery(query);
        return resultSet;
        }
        catch(Exception e){
            return null;
        }
    }
}
