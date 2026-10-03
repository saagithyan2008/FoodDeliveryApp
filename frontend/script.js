
let foods = [];
let cart = [];

async function loadFoods() {
  try {
    const response = await fetch("/api/foods");
    if (!response.ok) throw new Error("Failed to load menu");
    foods = await response.json();
    displayFoods(foods);
  } catch (error) {
    document.getElementById("food-list").textContent =
      "Unable to load menu. Please start the Java server.";
  }
}

function displayFoods(list) {
  const container = document.getElementById("food-list");
  container.innerHTML = "";

  list.forEach(food => {
    const card = document.createElement("div");
    card.className = "food-card";

    const img = document.createElement("img");
    img.src = food.image;
    img.alt = food.name;

    const name = document.createElement("h3");
    name.textContent = food.name;

    const price = document.createElement("p");
    price.textContent = "₹" + food.price;

    const button = document.createElement("button");
    button.textContent = "Add to Cart";
    button.addEventListener("click", () => addToCart(food.id));

    card.append(img, name, price, button);
    container.appendChild(card);
  });
}

function addToCart(id) {
  const food = foods.find(item => item.id === id);
  if (!food) return;

  const existing = cart.find(item => item.id === id);
  if (existing) {
    existing.quantity++;
  } else {
    cart.push({ ...food, quantity: 1 });
  }

  updateCart();
}

function changeQuantity(id, amount) {
  const item = cart.find(food => food.id === id);
  if (!item) return;

  item.quantity += amount;
  if (item.quantity <= 0) {
    cart = cart.filter(food => food.id !== id);
  }

  updateCart();
}

function updateCart() {
  const container = document.getElementById("cart-items");
  container.innerHTML = "";

  let total = 0;
  let count = 0;

  cart.forEach(item => {
    total += item.price * item.quantity;
    count += item.quantity;

    const row = document.createElement("div");
    row.className = "cart-row";

    const name = document.createElement("span");
    name.textContent = `${item.name} × ${item.quantity}`;

    const price = document.createElement("span");
    price.textContent = "₹" + item.price * item.quantity;

    const controls = document.createElement("div");
    const minus = document.createElement("button");
    minus.textContent = "−";
    minus.addEventListener("click", () =>
      changeQuantity(item.id, -1)
    );

    const plus = document.createElement("button");
    plus.textContent = "+";
    plus.addEventListener("click", () =>
      changeQuantity(item.id, 1)
    );

    controls.append(minus, plus);
    row.append(name, price, controls);
    container.appendChild(row);
  });

  if (cart.length === 0) {
    container.textContent = "Your cart is empty.";
  }

  document.getElementById("total").textContent = total;
  document.getElementById("cart-count").textContent = count;
}

function showCart() {
  document.getElementById("cart").scrollIntoView({
    behavior: "smooth"
  });
}

document.getElementById("search").addEventListener("input", event => {
  const query = event.target.value.toLowerCase();
  displayFoods(
    foods.filter(food => food.name.toLowerCase().includes(query))
  );
});

async function placeOrder() {
  const customer = document.getElementById("customer").value.trim();
  const message = document.getElementById("message");

  if (!customer) {
    message.textContent = "Please enter your name.";
    return;
  }

  if (cart.length === 0) {
    message.textContent = "Your cart is empty.";
    return;
  }

  const order = {
    customer,
    items: cart.map(item => ({
      id: item.id,
      quantity: item.quantity
    }))
  };

  try {
    const response = await fetch("/api/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(order)
    });

    const result = await response.json();

    if (!response.ok) throw new Error(result.error || "Order failed");

    message.textContent = `Order placed! Order ID: ${result.orderId}`;
    cart = [];
    updateCart();
    document.getElementById("customer").value = "";
  } catch (error) {
    message.textContent = error.message;
  }
}

loadFoods();