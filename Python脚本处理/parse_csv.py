import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

csv_path = './tournament-20220107-203655-party_domain.log.csv'

data_frame = pd.read_csv(csv_path, sep=';')

n_rows, n_columns = data_frame.shape

agent1 = data_frame['Agent 1'].tolist()
agent2 = data_frame['Agent 2'].tolist()

profile1 = data_frame['Profile 1']
profile2 = data_frame['Profile 2']
total_agents = set([agent.split("@")[0] for agent in agent1 + agent2])

print(total_agents)
print(data_frame.columns.values.tolist())


def findAll(agentName='Agent17Mu'):
    agent1_no_appedix = [agent.split("@")[0] for agent in agent1]
    agentMuIdx_front = []
    for idx, item in enumerate(agent1_no_appedix):
        if item == agentName:
            agentMuIdx_front.append(idx)

    front = data_frame.iloc[agentMuIdx_front]

    agent2_no_appedix = [agent.split("@")[0] for agent in agent2]
    agentMuIdx_behind = []
    for idx, item in enumerate(agent2_no_appedix):
        if item == agentName:
            agentMuIdx_behind.append(idx)

    behind = data_frame.iloc[agentMuIdx_behind]

    return pd.concat([front, behind], axis=0)


def plotComparisonImage(CompareBundle, xlables, labels):
    '''

    :param CompareBundle: 需要对比的agent
    :param xlables: 需要跑那些数据
    :param labels: agent的所有名字
    :return:
    '''
    outcomes = []
    for i in CompareBundle:
        Agent17Mu = findAll(i)
        DistanceNash = Agent17Mu['Dist. to Nash'].mean()
        DistancePareto = Agent17Mu['Dist. to Pareto'].mean()
        Welfare = Agent17Mu['Social Welfare'].mean()
        outcomes.append([DistanceNash, DistancePareto, Welfare])

        # print(f"{DistanceNash:.4f},{DistancePareto:.4f},{Welfare:.4f}")

    X = np.arange(len(xlables))
    bar_interval = 0.25
    bar_width = (1 - 0.25) / len(labels)
    print(bar_width)

    for idx, o in enumerate(outcomes):
        plt.bar(X + idx * bar_width, o, width=bar_width, label=labels[idx])

    plt.tight_layout()
    plt.legend()
    plt.xticks(X + (bar_width * len(labels)) / 2, xlables)
    plt.show()


if __name__ == '__main__':
    xlables = ['Distance to Nash point', 'Distance to Pareto Frontier', 'Social Welfare']
    labels = ['mutation=0.08', 'mutation=0.5', 'mutation=0.1']
    plotComparisonImage(['Agent17Mu', 'Agent17Mu2', 'Agent17Mu3'], xlables, labels)

    plotComparisonImage(['Agent17PS', 'Agent17PS2', 'Agent17PS3'], xlables,
                        ['Population size 50', 'Population size 500', 'Population size 5000'])

    plotComparisonImage(
        ['BoulwareNegotiationParty', 'Agent17Submit', 'AgreeableAgent2018', 'Agent17XNoStop', 'Agent17X'], xlables,
        ['BoulwareNegotiationParty', 'Agent17 before', 'AgreeableAgent2018', 'Agent17 None Early-stop', 'Agent17'])
