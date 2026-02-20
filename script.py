import csv

# Share tea shop menu items
menu_items = [
    ["menu_item_id", "name", "category", "base_price", "is_active"],
    [1, "Classic Milk Tea", "Milk Tea", 4.75, True],
    [2, "Taro Milk Tea", "Milk Tea", 5.25, True],
    [3, "Thai Tea", "Milk Tea", 5.00, True],
    [4, "Jasmine Green Tea", "Brewed Tea", 3.75, True],
    [5, "Oolong Tea", "Brewed Tea", 3.75, True],
    [6, "Wintermelon Tea", "Fruit Tea", 4.50, True],
    [7, "Mango Green Tea", "Fruit Tea", 5.25, True],
    [8, "Strawberry Tea", "Fruit Tea", 5.25, True],
    [9, "Passion Fruit Tea", "Fruit Tea", 5.00, True],
    [10, "Brown Sugar Boba Milk", "Specialty", 5.75, True],
    [11, "Matcha Latte", "Specialty", 5.75, True],
    [12, "Honey Lemon Tea", "Fruit Tea", 4.75, True],
    [13, "Lychee Tea", "Fruit Tea", 5.00, True],
    [14, "Peach Oolong Tea", "Fruit Tea", 5.25, True],
    [15, "Brown Sugar Milk Tea", "Milk Tea", 5.50, True],
    [16, "Mango Milk Tea", "Milk Tea", 5.50, True],
    [17, "Strawberry Milk Tea", "Milk Tea", 5.50, True],
    [18, "Honeydew Milk Tea", "Milk Tea", 5.50, True],
    [19, "Wintermelon Milk Tea", "Milk Tea", 5.50, True],
    [20, "Grape Chia", "Milk Tea", 5.50, True],
    [21, "Passion Fruit", "Milk Tea", 5.50, True],
    [22, "Oolong Milk Tea", "Milk Tea", 5.50, True],
    [23, "Honey Lemon Milk Tea", "Milk Tea", 5.50, True],
    [24, "Peach Milk Tea", "Milk Tea", 5.50, True]
    
]

with open("menu_items.csv", "w", newline="", encoding="utf-8") as f:
    writer = csv.writer(f)
    writer.writerows(menu_items)