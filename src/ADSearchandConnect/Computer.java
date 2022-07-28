/*
 * Computer Object to store info about computers we find
 */
package ADSearchandConnect;
/*
 * Gabriel Brown
 * City of Gulfport
 */
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
public class Computer
{
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    LocalDateTime now = LocalDateTime.now();

    /* Private Instance Variables */
    private String PCname; /* PC Name format example: IT2200936 where it is Department abbreviation + PC asset number */
    private String description; /* Comes from AD Description. If lucky, this string will contain full name and job title. */
    private final String dateNtime; /* Uses date and time formatter to create string with simple date and time for our PC history  */

    /* Default, Empty Constructor. */
    public Computer()
    { this.PCname = "Unknown"; this.description = "Unknown"; this.dateNtime = dtf.format(now); }

    /* Constructor which only passes PC Name. */
    public Computer(String name)
    { this.PCname = name; this.description = "Unknown"; this.dateNtime = dtf.format(now); }

    /* Full Constructor */
    public Computer(String name, String description)
    {
        this.PCname = name;
        this.description = description;
        this.dateNtime = dtf.format(now);
    }

    /* Sets */
    public void setPCname(String PCname) { this.PCname = PCname; }
    public void setUser(String description) { this.description = description; }

    /* Gets */
    public String getPCname() { return PCname; }
    public String getDescription() { return description; }
    public String getDateNtime() { return dateNtime; }

}