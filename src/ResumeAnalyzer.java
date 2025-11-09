import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResumeAnalyzer {

    static Connection con; // Database connection object

    public static void main(String[] args) {
        // Initialize database first
        initializeDatabase();
        
        // Then setup the GUI
        setupGUI();
    }

    private static void initializeDatabase() {
        try {
            String dburl = "jdbc:postgresql://localhost:5432/resume_analyzer";
            String dbusername = "postgres";
            String dbpassword = "Ohmsairam1!";
            con = DriverManager.getConnection(dburl, dbusername, dbpassword);
            System.out.println("Database Connected!");

            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS candidates (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "skills TEXT, " +
                "experience INTEGER, " +
                "score INTEGER, " +
                "status VARCHAR(20) DEFAULT 'New', " +
                "created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            
            Statement stmt = con.createStatement();
            stmt.execute(createTableSQL);
            System.out.println("Table 'candidates' verified/created successfully!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Database initialization failed!\n" + e.getMessage() + 
                "\n\nPlease ensure:\n1. PostgreSQL is running\n2. Database 'resume_analyzer' exists\n3. Credentials are correct", 
                "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }


    private static void setupGUI() {
        // Frame setup
        JFrame frame = new JFrame("Resume Analyzer");
        frame.setSize(650, 600);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Labels
        JLabel l1 = new JLabel("Candidate Name");
        l1.setBounds(50, 50, 150, 25);
        JLabel l2 = new JLabel("Skills");
        l2.setBounds(50, 90, 150, 25);
        JLabel l3 = new JLabel("Experience (years)");
        l3.setBounds(50, 130, 150, 25);
        JLabel l4 = new JLabel("Status");
        l4.setBounds(50, 170, 150, 25);

        // Text fields
        JTextField tb1 = new JTextField(30);
        tb1.setBounds(200, 50, 250, 25);
        JTextField tb2 = new JTextField(30);
        tb2.setBounds(200, 90, 250, 25);
        JTextField tb3 = new JTextField(10);
        tb3.setBounds(200, 130, 250, 25);

        // Status ComboBox
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"New", "Screening", "Interview", "Rejected", "Hired"});
        statusCombo.setBounds(200, 170, 250, 25);

        // Buttons
        JButton analyzeBtn = new JButton("Analyze & Save");
        analyzeBtn.setBounds(200, 210, 160, 30);

        JButton searchBtn = new JButton("Search");
        searchBtn.setBounds(50, 260, 100, 25);

        JButton updateBtn = new JButton("Update");
        updateBtn.setBounds(160, 260, 100, 25);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBounds(270, 260, 100, 25);

        JButton statsBtn = new JButton("Show Stats");
        statsBtn.setBounds(380, 260, 120, 25);

        JButton showAllBtn = new JButton("Show All");
        showAllBtn.setBounds(230, 300, 120, 25);

        JButton advancedSearchBtn = new JButton("Advanced Search");
        advancedSearchBtn.setBounds(50, 340, 150, 25);

        JButton exportBtn = new JButton("Export Data");
        exportBtn.setBounds(210, 340, 150, 25);

        JButton resetDbBtn = new JButton("Reset Database");
        resetDbBtn.setBounds(370, 340, 150, 25);

        JLabel resultLabel = new JLabel("");
        resultLabel.setBounds(150, 380, 400, 25);

        JLabel dbStatusLabel = new JLabel("✓ Database Connected");
        dbStatusLabel.setBounds(50, 420, 300, 25);
        dbStatusLabel.setForeground(Color.GREEN);

        // Step 2: Analyze & Save
        analyzeBtn.addActionListener(event -> {
            String name = tb1.getText();
            String skills = tb2.getText();
            String expText = tb3.getText();
            String status = (String) statusCombo.getSelectedItem();

            if (name.isEmpty() || skills.isEmpty() || expText.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "All fields are required!");
                return;
            }

            try {
                int exp = Integer.parseInt(expText);
                int score = calculateScore(skills, exp);

                String query = "INSERT INTO candidates(name, skills, experience, score, status) VALUES(?, ?, ?, ?, ?)";
                PreparedStatement pstmt = con.prepareStatement(query);
                pstmt.setString(1, name);
                pstmt.setString(2, skills);
                pstmt.setInt(3, exp);
                pstmt.setInt(4, score);
                pstmt.setString(5, status);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(frame, "Resume analyzed & saved!\nScore: " + score + "/100\nStatus: " + status);
                resultLabel.setText("Candidate Score: " + score + "/100 - Status: " + status);

                tb1.setText("");
                tb2.setText("");
                tb3.setText("");
                statusCombo.setSelectedIndex(0);

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(frame, "Experience must be a number!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Database error: " + e.getMessage());
            }
        });

        // Search Candidate by Name
        searchBtn.addActionListener(e -> {
            String searchName = JOptionPane.showInputDialog("Enter candidate name to search:");
            if (searchName == null || searchName.isEmpty()) return;

            try {
                String query = "SELECT * FROM candidates WHERE LOWER(name) LIKE LOWER(?)";
                PreparedStatement pstmt = con.prepareStatement(query);
                pstmt.setString(1, "%" + searchName + "%");
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    StringBuilder candidateInfo = new StringBuilder();
                    candidateInfo.append("Name: ").append(rs.getString("name"))
                                .append("\nSkills: ").append(rs.getString("skills"))
                                .append("\nExperience: ").append(rs.getInt("experience")).append(" years")
                                .append("\nScore: ").append(rs.getInt("score")).append("/100")
                                .append("\nStatus: ").append(rs.getString("status"))
                                .append("\nAdded: ").append(rs.getTimestamp("created_date"));
                    
                    JOptionPane.showMessageDialog(frame, candidateInfo.toString(), "Candidate Found", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "No candidate found with name: " + searchName);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Search error: " + ex.getMessage());
            }
        });

        // Update Candidate
        updateBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter candidate name to update:");
            if (name == null || name.isEmpty()) return;

            // First, get current values
            try {
                String selectQuery = "SELECT * FROM candidates WHERE LOWER(name) = LOWER(?)";
                PreparedStatement selectStmt = con.prepareStatement(selectQuery);
                selectStmt.setString(1, name);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(frame, "Candidate not found!");
                    return;
                }

                String currentSkills = rs.getString("skills");
                int currentExp = rs.getInt("experience");
                String currentStatus = rs.getString("status");

                String newSkill = JOptionPane.showInputDialog("Enter new skills:", currentSkills);
                if (newSkill == null) return;

                String newExpStr = JOptionPane.showInputDialog("Enter new experience:", currentExp);
                if (newExpStr == null) return;

                String newStatus = (String) JOptionPane.showInputDialog(frame, 
                        "Select new status:", "Update Status", 
                        JOptionPane.QUESTION_MESSAGE, null, 
                        new String[]{"New", "Screening", "Interview", "Rejected", "Hired"}, 
                        currentStatus);

                if (newStatus == null) return;

                try {
                    int newExp = Integer.parseInt(newExpStr);
                    int newScore = calculateScore(newSkill, newExp);
                    
                    String updateQuery = "UPDATE candidates SET skills = ?, experience = ?, score = ?, status = ? WHERE LOWER(name) = LOWER(?)";
                    PreparedStatement pstmt = con.prepareStatement(updateQuery);
                    pstmt.setString(1, newSkill);
                    pstmt.setInt(2, newExp);
                    pstmt.setInt(3, newScore);
                    pstmt.setString(4, newStatus);
                    pstmt.setString(5, name);
                    int rows = pstmt.executeUpdate();

                    if (rows > 0)
                        JOptionPane.showMessageDialog(frame, "Candidate updated successfully!\nNew Score: " + newScore + "/100");
                    else
                        JOptionPane.showMessageDialog(frame, "Candidate not found!");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Experience must be a number!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Update error: " + ex.getMessage());
            }
        });

        // Delete Candidate
        deleteBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter candidate name to delete:");
            if (name == null || name.isEmpty()) return;

            int confirm = JOptionPane.showConfirmDialog(frame, 
                    "Are you sure you want to delete candidate: " + name + "?", 
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String query = "DELETE FROM candidates WHERE LOWER(name) = LOWER(?)";
                    PreparedStatement pstmt = con.prepareStatement(query);
                    pstmt.setString(1, name);
                    int rows = pstmt.executeUpdate();

                    if (rows > 0)
                        JOptionPane.showMessageDialog(frame, "Candidate deleted successfully!");
                    else
                        JOptionPane.showMessageDialog(frame, "Candidate not found!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Delete error: " + ex.getMessage());
                }
            }
        });

        // Enhanced Statistics
        statsBtn.addActionListener(e -> {
            try {
                String query = "SELECT " +
                              "COUNT(*) as total, " +
                              "AVG(score) as avg_score, " +
                              "MAX(score) as max_score, " +
                              "MIN(score) as min_score, " +
                              "status, " +
                              "COUNT(*) FILTER (WHERE score >= 70) as high_scorers " +
                              "FROM candidates GROUP BY status";
                
                PreparedStatement pstmt = con.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery();
                
                StringBuilder stats = new StringBuilder("=== Resume Analysis Statistics ===\n\n");
                
                // Overall stats
                Statement overallStmt = con.createStatement();
                ResultSet overall = overallStmt.executeQuery(
                    "SELECT COUNT(*) as total, AVG(score) as avg_score, " +
                    "MAX(score) as max_score, MIN(score) as min_score, " +
                    "COUNT(*) FILTER (WHERE score >= 70) as high_scorers FROM candidates");
                
                if (overall.next()) {
                    stats.append("Overall Statistics:\n")
                        .append("Total Candidates: ").append(overall.getInt("total")).append("\n")
                        .append("Average Score: ").append(String.format("%.2f", overall.getDouble("avg_score"))).append("/100\n")
                        .append("Highest Score: ").append(overall.getInt("max_score")).append("/100\n")
                        .append("Lowest Score: ").append(overall.getInt("min_score")).append("/100\n")
                        .append("High Scorers (70+): ").append(overall.getInt("high_scorers")).append("\n\n");
                }
                
                // Status distribution
                stats.append("Status Distribution:\n");
                int totalCandidates = 0;
                boolean hasResults = false;
                while (rs.next()) {
                    hasResults = true;
                    int count = rs.getInt("total");
                    stats.append("• ").append(rs.getString("status")).append(": ")
                        .append(count).append(" candidates");
                    
                    int highScorers = rs.getInt("high_scorers");
                    if (highScorers > 0) {
                        stats.append(" (").append(highScorers).append(" high scorers)");
                    }
                    stats.append("\n");
                    totalCandidates += count;
                }
                
                if (!hasResults) {
                    stats.append("No candidates in database yet.\n");
                }
                
                // Top candidate
                ResultSet top = overallStmt.executeQuery(
                    "SELECT name, score FROM candidates ORDER BY score DESC LIMIT 1");
                if (top.next()) {
                    stats.append("\n🏆 Top Candidate: ").append(top.getString("name"))
                         .append(" (").append(top.getInt("score")).append("/100)");
                }
                
                JTextArea textArea = new JTextArea(stats.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(450, 350));
                
                JOptionPane.showMessageDialog(frame, scrollPane, "Detailed Statistics", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error generating statistics: " + ex.getMessage());
            }
        });

        // Show All Candidates in JTable
        showAllBtn.addActionListener(e -> {
            JFrame tableFrame = new JFrame("All Candidates");
            tableFrame.setSize(800, 400);

            String[] columns = {"ID", "Name", "Skills", "Experience", "Score", "Status", "Created Date"};
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Make table non-editable
                }
            };
            JTable table = new JTable(model);
            table.setAutoCreateRowSorter(true); // Enable sorting

            try {
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM candidates ORDER BY id ASC");

                int count = 0;
                while (rs.next()) {
                    Object[] row = {
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("skills"),
                            rs.getInt("experience"),
                            rs.getInt("score"),
                            rs.getString("status"),
                            rs.getTimestamp("created_date")
                    };
                    model.addRow(row);
                    count++;
                }
                
                tableFrame.setTitle("All Candidates - " + count + " records");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error loading data: " + ex.getMessage());
            }

            JScrollPane scrollPane = new JScrollPane(table);
            tableFrame.add(scrollPane);
            tableFrame.setVisible(true);
        });

        // Advanced Search
        advancedSearchBtn.addActionListener(e -> {
            JDialog searchDialog = new JDialog(frame, "Advanced Search", true);
            searchDialog.setLayout(new GridLayout(0, 2, 10, 10));
            searchDialog.setSize(400, 300);
            
            JTextField nameField = new JTextField();
            JTextField skillsField = new JTextField();
            JSpinner minExpSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
            JSpinner minScoreSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
            JComboBox<String> statusSearchCombo = new JComboBox<>(new String[]{"Any", "New", "Screening", "Interview", "Rejected", "Hired"});
            
            searchDialog.add(new JLabel("Name:"));
            searchDialog.add(nameField);
            searchDialog.add(new JLabel("Skills:"));
            searchDialog.add(skillsField);
            searchDialog.add(new JLabel("Min Experience:"));
            searchDialog.add(minExpSpinner);
            searchDialog.add(new JLabel("Min Score:"));
            searchDialog.add(minScoreSpinner);
            searchDialog.add(new JLabel("Status:"));
            searchDialog.add(statusSearchCombo);
            
            JButton searchDialogBtn = new JButton("Search");
            JButton cancelBtn = new JButton("Cancel");
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(searchDialogBtn);
            buttonPanel.add(cancelBtn);
            
            searchDialog.add(new JLabel()); // empty cell
            searchDialog.add(buttonPanel);
            
            searchDialogBtn.addActionListener(ev -> {
                try {
                    StringBuilder query = new StringBuilder("SELECT * FROM candidates WHERE 1=1");
                    List<Object> params = new ArrayList<>();
                    
                    if (!nameField.getText().isEmpty()) {
                        query.append(" AND LOWER(name) LIKE LOWER(?)");
                        params.add("%" + nameField.getText() + "%");
                    }
                    if (!skillsField.getText().isEmpty()) {
                        query.append(" AND LOWER(skills) LIKE LOWER(?)");
                        params.add("%" + skillsField.getText() + "%");
                    }
                    if ((int) minExpSpinner.getValue() > 0) {
                        query.append(" AND experience >= ?");
                        params.add(minExpSpinner.getValue());
                    }
                    if ((int) minScoreSpinner.getValue() > 0) {
                        query.append(" AND score >= ?");
                        params.add(minScoreSpinner.getValue());
                    }
                    if (!statusSearchCombo.getSelectedItem().equals("Any")) {
                        query.append(" AND status = ?");
                        params.add(statusSearchCombo.getSelectedItem());
                    }
                    
                    query.append(" ORDER BY score DESC");
                    
                    PreparedStatement pstmt = con.prepareStatement(query.toString());
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setObject(i + 1, params.get(i));
                    }
                    
                    ResultSet rs = pstmt.executeQuery();
                    displaySearchResults(rs, searchDialog);
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(searchDialog, "Search error: " + ex.getMessage());
                }
            });
            
            cancelBtn.addActionListener(ev -> searchDialog.dispose());
            
            searchDialog.setVisible(true);
        });

        // Export Data
        exportBtn.addActionListener(e -> {
            try {
                String[] options = {"CSV", "Cancel"};
                int choice = JOptionPane.showOptionDialog(frame, "Export format:", "Export Data",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                
                if (choice == 0) exportToCSV(frame);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage());
            }
        });

        // Reset Database
        resetDbBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "This will DELETE ALL CANDIDATES and reset the database!\nAre you sure?",
                    "Reset Database", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    Statement stmt = con.createStatement();
                    stmt.execute("DELETE FROM candidates");
                    stmt.execute("ALTER SEQUENCE candidates_id_seq RESTART WITH 1");
                    
                    JOptionPane.showMessageDialog(frame, 
                        "Database reset successfully!\nAll candidate data has been cleared.",
                        "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Reset failed: " + ex.getMessage());
                }
            }
        });

        // Add components
        frame.add(l1);
        frame.add(l2);
        frame.add(l3);
        frame.add(l4);
        frame.add(tb1);
        frame.add(tb2);
        frame.add(tb3);
        frame.add(statusCombo);
        frame.add(analyzeBtn);
        frame.add(searchBtn);
        frame.add(updateBtn);
        frame.add(deleteBtn);
        frame.add(statsBtn);
        frame.add(showAllBtn);
        frame.add(advancedSearchBtn);
        frame.add(exportBtn);
        frame.add(resetDbBtn);
        frame.add(resultLabel);
        frame.add(dbStatusLabel);

        frame.setVisible(true);
    }

    // Enhanced Scoring System
    private static int calculateScore(String skills, int experience) {
        int score = 0;
        
        // Technical skills scoring
        String[] techSkills = {"java", "python", "sql", "javascript", "spring", "html", "css", 
                              "react", "angular", "node.js", "docker", "aws", "azure", "git"};
        int[] skillWeights = {30, 25, 25, 20, 25, 15, 15, 25, 25, 25, 20, 25, 20, 15};
        
        String lowerSkills = skills.toLowerCase();
        for (int i = 0; i < techSkills.length; i++) {
            if (lowerSkills.contains(techSkills[i])) {
                score += skillWeights[i];
            }
        }
        
        // Experience scoring (capped)
        score += Math.min(experience * 5, 30);
        
        return Math.min(score, 100); // Cap at 100
    }

    // Display search results
    private static void displaySearchResults(ResultSet rs, JDialog parent) throws SQLException {
        JFrame resultFrame = new JFrame("Search Results");
        resultFrame.setSize(800, 400);

        String[] columns = {"ID", "Name", "Skills", "Experience", "Score", "Status", "Created Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);

        int resultCount = 0;
        while (rs.next()) {
            Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("skills"),
                    rs.getInt("experience"),
                    rs.getInt("score"),
                    rs.getString("status"),
                    rs.getTimestamp("created_date")
            };
            model.addRow(row);
            resultCount++;
        }

        JScrollPane scrollPane = new JScrollPane(table);
        resultFrame.add(scrollPane, BorderLayout.CENTER);
        
        JLabel countLabel = new JLabel("Found " + resultCount + " candidate(s)");
        resultFrame.add(countLabel, BorderLayout.SOUTH);
        
        resultFrame.setVisible(true);
        parent.dispose();
    }

    // Export to CSV
    private static void exportToCSV(JFrame parent) throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export to CSV");
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new java.io.File(file.getAbsolutePath() + ".csv");
            }
            
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("ID,Name,Skills,Experience,Score,Status,CreatedDate");
                
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM candidates ORDER BY id ASC");
                
                int count = 0;
                while (rs.next()) {
                    writer.printf("%d,\"%s\",\"%s\",%d,%d,%s,%s%n",
                        rs.getInt("id"),
                        rs.getString("name").replace("\"", "\"\""),
                        rs.getString("skills").replace("\"", "\"\""),
                        rs.getInt("experience"),
                        rs.getInt("score"),
                        rs.getString("status"),
                        rs.getTimestamp("created_date"));
                    count++;
                }
                
                JOptionPane.showMessageDialog(parent, 
                    "Data exported successfully!\n" +
                    "File: " + file.getAbsolutePath() + "\n" +
                    "Records: " + count, 
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}