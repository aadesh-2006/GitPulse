const SELECTED_REPO_KEY = 'gitpulse:selectedRepositoryId';

export const storage = {
  getSelectedRepositoryId(): number | null {
    try {
      const value = localStorage.getItem(SELECTED_REPO_KEY);
      if (!value) return null;
      const parsed = parseInt(value, 10);
      return Number.isNaN(parsed) ? null : parsed;
    } catch {
      return null;
    }
  },

  setSelectedRepositoryId(id: number | null): void {
    try {
      if (id === null) {
        localStorage.removeItem(SELECTED_REPO_KEY);
      } else {
        localStorage.setItem(SELECTED_REPO_KEY, String(id));
      }
    } catch {
      // Ignore storage errors in restricted contexts
    }
  },
};
