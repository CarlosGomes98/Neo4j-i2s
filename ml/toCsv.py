def buildCSV():
    r = open("data.txt", "r")
    w = open("data.csv", "w+")
    fl = r.readlines()

    for index, line in enumerate(fl):
        adjustedIndex = index + 1
        w.write(line.rstrip("\n"))

        if adjustedIndex % 14 == 0 and adjustedIndex != 1:
            w.write("\n")
        else:
            w.write(",")