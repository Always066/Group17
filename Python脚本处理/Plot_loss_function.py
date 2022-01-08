path = "./dd.txt"

f = open(path, mode='r', encoding='utf-8')

total_list = []
sublist = []
for i in f.readlines():
    item_line = i.strip()

    if item_line == "":
        print("ok")
        total_list.append(sublist)
        sublist = []
    else:
        num = (float)(item_line)
        sublist.append(num)

f.close()

import matplotlib.pyplot as plt

label_dict = {0: "Early stop case1", 1: "Early stop case2", 2: "None Early stop"}
for idx, sublist in enumerate(total_list):
    plt.plot(sublist, label=label_dict[idx])
plt.legend()

plt.xlabel("iteration number")
plt.ylabel("fitness value")
plt.title("Comparison of early stopping and non-early stopping")
plt.savefig("dd.png")
plt.show()

