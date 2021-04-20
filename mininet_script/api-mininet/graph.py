import random
from io import StringIO


class Graph(object):

    """ Directed, acyclic graph with edge weights.
    Graph can be constructed two different ways. Option 1 is to create an empty
    graph and add edges using `add_edge(u, w, v)` method. For example, to
    create graph G connecting node 0 to node 1 with edge weight 5, and node 1
    to node 2 with edge weight 3, i.e.
           5      3
        0 ---> 1 ---> 2
    >>> G = Graph()
    >>> G.add_edge(0, 1, 5)
    >>> G.add_edge(1, 2, 3)
    Another option is to pass adjacency list and edge weights directly as
    dictionaries. The same example with that way is constructed as:
    >>> adjacency_list = {0: 1, 1: 2}
    >>> edge_weights = {(0, 1): 5, (1, 2): 3}
    >>> G = Graph(adjacency_list, edge_weights)
    """

    def __init__(self, adjacency_list=dict(), edge_weights=dict()):
        self.__adjacency_list = adjacency_list.copy()
        self.__edge_weights = edge_weights.copy()

    def add_edge(self, u, v, w):
        """ Add a new edge u -> v to graph with edge weight w. """
        self.__edge_weights[u, v] = w
        if u not in self.__adjacency_list:
            self.__adjacency_list[u] = set()
        self.__adjacency_list[u].add(v)

    def get_edge_weight(self, u, v):
        """ Get edge weight of edge between u and v. """
        return self.__edge_weights[u, v]

    def get_adjacent_nodes(self, u):
        """ Get nodes adjacent to u. """
        return self.__adjacency_list.get(u, set())

    def get_number_of_nodes(self):
        """ Return the total number of nodes in graph. """
        return len(self.__adjacency_list)

    def get_nodes(self):
        """ Return all nodes in this graph. """
        return self.__adjacency_list.keys()

    def __str__(self):
        io = StringIO()
        N = self.get_number_of_nodes()
        print("Directed, acyclic graph with %d nodes" % N, file=io)
        for u in self.get_nodes():
            adj = self.get_adjacent_nodes(u)
            print("Node %s: connected to %d nodes" % (u, len(adj)), file=io)
        return io.getvalue()


def generate_random_graph(nvertices, edge_density, min_weight, max_weight):
    """ Generate random graph with `nvertices`. """
    G = Graph()
    for u in range(nvertices):
        nadj = int(nvertices * edge_density)
        while len(G.get_adjacent_nodes(u)) < nadj:
            v = random.randrange(nvertices)
            if u == v:
                continue
            w = random.randint(min_weight, max_weight)
            G.add_edge(u, v, w)
    return G