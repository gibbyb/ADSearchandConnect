/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                         *
 *   Console Application that will search Active Directory for the specific PC the user    *
 *   is searching for from keywords from the PC name, or the users name, or department.    *
 *   Once PC is found, user can automatically connect to the PC. Application also logs     *
 *   the history of all PCs connected to it. Once disconnected from selected PC, if        *
 *   PC has no description, or lacks information, you can change the description in        *
 *   Active Directory directly from this program as well.                                  *
 *                                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package ADSearchandConnect;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellNotAvailableException;
import com.profesorfalken.jpowershell.PowerShellResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/* * * * * * * * * * * * * * * * * * * * * * * * *
 *                                               *
 *      Active Directory PC Search & Connect     *
 *              By Gabriel Brown                 *
 *              City of Gulfport                 *
 *                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * */
public class Solution
{
    public static void main(String[] args) throws IOException
    {
        printTitle();
        /* Starting program by trying to run jPowershell, an essential dependency for this app */
        try(PowerShell powerShell = PowerShell.openSession())
        {
            /* Creating an arraylist to store our Computer objects from search results. */
            ArrayList<Computer> allPCs = new ArrayList<>();

            /* Initialize our history text file */
            File historyTxt = new File("PCHistory.txt");

            boolean run = true;
            while (run)
            {
                String input = getInput();

                switch (input)
                {
                    case "":
                    case "n":
                        run = false; break;
                    default:
                        searchAD(input, allPCs, powerShell);
                        if (allPCs.isEmpty())
                        { System.out.println("No results found!"); break; }
                        printPCs(allPCs);
                        String PCname = selectPC(allPCs, historyTxt);
                        if (!(PCname.equals("Unknown")))
                        {
                            connectToPC(PCname, powerShell);
                            changeDescription(PCname, powerShell);
                        }
                }
            }
        }
        catch (PowerShellNotAvailableException ex)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("JPowerShell could not open a PowerShell session.");
            ex.printStackTrace();
            reader.readLine();
        }
    }

    private static String getInput() throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("\nEnter keyword: ");
        return reader.readLine();
    }

    /* searchAD takes in the user input and adds it to a longer string that is a powershell
     * command which will search Active Directory for our PC. This function creates Computer
     * objects and adds them to our arraylist of computer objects. These objects are our
     * search results. */
    private static void searchAD(String input, ArrayList<Computer> allPCs, PowerShell powerShell)
    {
        String keyword = "description";
        if (input.matches("\\d+") || input.matches("\\w\\w\\d+") || input.matches("\\w\\w"))
            keyword = "name";

        String nameSearch = "(Get-ADComputer -Filter '" + keyword + " -like \"*" + input +
                "*\"' | Select Name | Select-String -Pattern \"\\w+\\d\" -List).Matches.Value";

        String descriptionSearch = "(Get-ADComputer -Filter '" + keyword + " -like \"*" + input +
                "*\"' -Properties * | Select Description | Out-String)" +
                ".Replace(\"Description\", \"\").Replace(\"-\",\"\").Replace(\" \", \"\")";

        System.out.print("Searching Active Directory.");

        PowerShellResponse nameResponse = powerShell.executeCommand(nameSearch);
        System.out.print(".");
        PowerShellResponse descriptionResponse = powerShell.executeCommand(descriptionSearch);
        System.out.print(".\n\n");

        Scanner nameOutput = new Scanner(nameResponse.getCommandOutput());
        Scanner descriptionOutput = new Scanner(descriptionResponse.getCommandOutput());

        while (nameOutput.hasNextLine())
        {
            if (descriptionOutput.hasNextLine())
            {
                Computer PC = new Computer(nameOutput.nextLine(), descriptionOutput.next());
                allPCs.add(PC);
            }
            else
            {
                Computer PC = new Computer(nameOutput.nextLine());
                allPCs.add(PC);
            }
        }

    }

    /* printPCInfo prints out our Computer objects stored after our search. */
    private static void printPCs(ArrayList<Computer> allPCs)
    {
        System.out.println("    PC Name\t\tPC Description");
        System.out.println("----------------------------------------------------------");
        int i = 1;
        for (Computer PC : allPCs)
        {
            if (i<10)
                System.out.println(" " + i + ". " + PC.getPCname() + "\t\t" + PC.getDescription());
            else
                System.out.println(i + ". " + PC.getPCname() + "\t\t" + PC.getDescription());
            i++;
        }
        System.out.println("----------------------------------------------------------");
    }

    /* The selectPC function prompts the user to select a PC from the list to connect to. If there is only one PC,
     * the user can either confirm that they want to connect to it, or decide not to, which will return a blank string
     * This function is also where our selected PC is added to our pcHistory arraylist of Computers, as if we select a
     * PC, we will be connecting to it, and therefore need to store it in our history. */
    private static String selectPC(ArrayList<Computer> allPCs, File historyTxt) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Computer PC = new Computer();
        String input;

        if (allPCs.size() == 1)
        {
            System.out.print("Connect to PC? ");
            input = reader.readLine();
            if (input.equalsIgnoreCase("y") || input.equals("1"))
            {
                PC = allPCs.get(0);
                try
                {
                    writeHistory(historyTxt, PC);
                    System.out.println(PC.getPCname() + " added to history log.");
                }
                catch (Exception ex)
                {
                    System.out.println("Error. Could not write history to the history file.");
                    ex.printStackTrace();
                    reader.readLine();
                }
            }
        }
        else
        {
            System.out.print("Select a PC to connect to. ");
            input = reader.readLine();
            if (input.matches("\\d+"))
            {
                int choice = Integer.parseInt(input) - 1;
                if (choice >= 0 && choice < allPCs.size())
                {
                    PC = allPCs.get(choice);
                    try
                    {
                        writeHistory(historyTxt, PC);
                        System.out.println(PC.getPCname() + " added to history log.");
                    }
                    catch (Exception ex)
                    {
                        System.out.println("Error. Could not write history to the history file.");
                        ex.printStackTrace();
                        reader.readLine();
                    }
                }
            }
        }
        allPCs.clear();
        return PC.getPCname();
    }

    /* writes to a text file, and appends to it */
    private static void writeHistory(File historyTxt, Computer PC) throws IOException
    {
        BufferedWriter histWriter = new BufferedWriter(new FileWriter(historyTxt, true));
        histWriter.write("\n\n\tDate & Time\t PC Name \t PC Description");
        histWriter.write("\n\t-----------------------------------------------------------------------");
        histWriter.write("\n\t" + PC.getDateNtime() + "\t " + PC.getPCname() + "\t " + PC.getDescription());
        histWriter.write("\n\t-----------------------------------------------------------------------");
        histWriter.close();
    }

    private static void connectToPC(String PCname, PowerShell powerShell)
    {
        System.out.print("Connecting now..");
        String connectCmd = "& \"C:\\Program Files (x86)\\SolarWinds\\DameWare Remote Support\\dwrcc.exe\" -c: -h: -m:{" + PCname + "} -a:1 -x";
        powerShell.executeCommand(connectCmd);
        System.out.print(".\n");
    }

    /* Function called after connecting to PC to allow the user to fix any errors
     * in the description if needed for any future searches. */
    private static void changeDescription(String PCname, PowerShell powerShell) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Change PC Description? ");
        String input = reader.readLine();
        if (input.equalsIgnoreCase("y") || input.equals("1"))
        {
            System.out.print("Enter user's full name: ");
            String fullName = reader.readLine();
            System.out.print("Enter user's job title: ");
            String jobTitle = reader.readLine();

            if (fullName.isBlank() && jobTitle.isBlank())
            {
                System.out.println("Description unchanged.");
                return;
            }
            else if (fullName.isBlank())
                fullName = "NA";
            else if (jobTitle.isBlank())
                jobTitle = "Unknown";

            String description = fullName + " | " + jobTitle;
            String setDescriptionCmd = "Set-ADComputer -Identity \"" + PCname +
                    "\" -Description \"" + description + "\"";

            powerShell.executeCommand(setDescriptionCmd);

            System.out.println(PCname + " description changed to \"" + description + "\"");
        }
        System.out.print("Open Track-it to create ticket? ");
        input = reader.readLine();
        if (input.equalsIgnoreCase("y") || input.equals("1"))
        {
            Runtime rt = Runtime.getRuntime();
            String url = "http://trackit/TrackIt/Account/LogIn?ReturnUrl=%2fTrackIt";
            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
        }
        else
        {
            System.out.println("Write a summary of support details for Track-it.\n");
            input = reader.readLine();
        }

    }

    /* Very much self-explanatory */
    private static void printTitle()
    {
        System.out.println();
        System.out.println("                                             ▄▀▄     ▄▀▄          ");
        System.out.println("    █▀▀ █ █▄▄ █▄▄ █▄█ █ █▀                  ▄█░░▀▀▀▀▀░░█▄          ");
        System.out.println("    █▄█ █ █▄█ █▄█  █    ▄█              ▄▄  █░░░░░░░░░░░█  ▄▄     ");
        System.out.println("                                       █▄▄█ █░░▀░░┬░░▀░░█ █▄▄█    ");
        System.out.println("██████╗  █████╗    ██████╗███████╗ █████╗ ██████╗░░█████╗ ██╗  ██╗");
        System.out.println("██╔══██╗██╔══██╗  ██╔════╝██╔════╝██╔══██╗██╔══██╗██╔══██╗██║  ██║");
        System.out.println("██████╔╝██║  ╚═╝  ╚█████╗ █████╗  ███████║██████╔╝██║  ╚═╝███████║");
        System.out.println("██╔═══╝ ██║  ██╗   ╚═══██╗██╔══╝  ██╔══██║██╔══██╗██║  ██╗██╔══██║");
        System.out.println("██║     ╚█████╔╝  ██████╔╝███████╗██║  ██║██║  ██║╚█████╔╝██║  ██║");
        System.out.println("╚═╝      ╚════╝   ╚═════╝ ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝ ╚════╝ ╚═╝  ╚═╝");
        System.out.println("        █████╗  █████╗ ███╗  ██╗███╗  ██╗███████╗ █████╗ ████████╗");
        System.out.println("  ██╗  ██╔══██╗██╔══██╗████╗ ██║████╗ ██║██╔════╝██╔══██╗╚══██╔══╝");
        System.out.println("██████╗██║  ╚═╝██║  ██║██╔██╗██║██╔██╗██║█████╗  ██║  ╚═╝   ██║   ");
        System.out.println("╚═██╔═╝██║  ██╗██║  ██║██║╚████║██║╚████║██╔══╝  ██║  ██╗   ██║   ");
        System.out.println("  ╚═╝  ╚█████╔╝╚█████╔╝██║ ╚███║██║ ╚███║███████╗╚█████╔╝   ██║   ");
        System.out.println(" ▓█▀▀▀▀▀╚════╝  ╚════╝ ╚═╝  ╚══╝╚═╝  ╚══╝╚══════╝ ╚════╝    ╚═╝   ");
        System.out.println(" ▓█░░▄░░▄░░░█▓ █████  █ █ █▀▀ █▀█ █▀ █ █▀█ █▄ █   ▄█   █▀█ ▀█     ");
        System.out.println(" ▓█▄▄▄▄▄▄▄▄▄█▓ █▄▄▄█  ▀▄▀ ██▄ █▀▄ ▄█ █ █▄█ █ ▀█    █ ▄ █▄█ █▄     ");
        System.out.println("    ▄▄███▄▄    █████                                              ");

    }
    
}
